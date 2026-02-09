//package com.example.PruebaPDFBox.controller;
//
//import java.awt.*;
//
//import com.example.PruebaPDFBox.model.ExtractPdf;
//import com.example.PruebaPDFBox.repository.extractPdfRepository;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.PDPage;
//import org.apache.pdfbox.pdmodel.PDResources;
//import org.apache.pdfbox.pdmodel.graphics.PDXObject;
//import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
//import org.apache.pdfbox.text.PDFTextStripper;
//import org.apache.pdfbox.text.PDFTextStripperByArea;
//import org.apache.pdfbox.text.TextPosition;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//import org.apache.pdfbox.cos.COSName;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.*;
//import java.util.List;
//
//import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
//
//@RestController
//@RequestMapping("/pdf2")
//public class extractPdfController {
//
//    @Autowired
//    private extractPdfRepository extractPdfRepository;
//
//    private static final String PDF_FOLDER = "pdfs";
//    private static final String IMAGES_FOLDER = "extracted_images";
//
//    @GetMapping("/extract-all")
//    public ResponseEntity<?> extractAll(@RequestParam String fileName) {
//        File file = new File(PDF_FOLDER + "/" + fileName);
//        if (!file.exists()) {
//            return ResponseEntity.badRequest().body("Archivo PDF no encontrado: " + fileName);
//        }
//
//        try (PDDocument document = PDDocument.load(file)) {
//            ExtractPdf entity = new ExtractPdf();
//            Map<String, String> campos = new HashMap<>();
//
//            // ===== EXTRAER IMÁGENES =====
//            List<String> rutasImagenes = extraerImagenes(document, fileName);
//            if (!rutasImagenes.isEmpty()) {
//                entity.setImagePaths(String.join(",", rutasImagenes));
//                entity.setImagenes(rutasImagenes);
//                entity.setTotalImagenes(rutasImagenes.size());
//            } else {
//                entity.setImagenes(new ArrayList<>());
//                entity.setTotalImagenes(0);
//            }
//
//            // Extraer el texto completo
//            PDFTextStripper fullStripper = new PDFTextStripper();
//            fullStripper.setSortByPosition(true);
//            String textoCompleto = fullStripper.getText(document);
//            entity.setTexto(textoCompleto);
//
//            // ===== CONFIGURACIÓN DE ÁREAS POR ETIQUETA =====
//            Map<String, AreaConfig> configuracionAreas = new HashMap<>();
//            configuracionAreas.put("Ciudad y Fecha:", new AreaConfig(52, 250, 12));
//            configuracionAreas.put("Tipo Evaluación Médica o Procedimiento:", new AreaConfig(150, 350, 12));
//            configuracionAreas.put("Empresa Donde Labora, Laborará o Laboró:", new AreaConfig(145, 180, 12));
//            configuracionAreas.put("Actividad Económica:", new AreaConfig(75, 120, 12));
//            configuracionAreas.put("CC:", new AreaConfig(-60, 40, 12));
//            configuracionAreas.put("Nombre del Trabajador a Aspirante:", new AreaConfig(120, 160, 12));
//            configuracionAreas.put("Genero:", new AreaConfig(30, 50, 12));
//            configuracionAreas.put("Edad:", new AreaConfig(20, 100, 12));
//            configuracionAreas.put("Estado Civil:", new AreaConfig(40, 50, 12));
//            configuracionAreas.put("Nivel Educativo:", new AreaConfig(60, 80, 12));
//            configuracionAreas.put("E.P.S:", new AreaConfig(20, 60, 12));
//            configuracionAreas.put("A.F.P:", new AreaConfig(20, 60, 12));
//            configuracionAreas.put("A.R.L:", new AreaConfig(20, 60, 12));
//            configuracionAreas.put("Tipo de Sangre:", new AreaConfig(52, 30, 12));
//            configuracionAreas.put("N° Hijos", new AreaConfig(25, 20, 12));
//            configuracionAreas.put("Ingresos:", new AreaConfig(-40, -10, 12));
//            configuracionAreas.put("Jornada Laboral:", new AreaConfig(60, 60, 12));
//            configuracionAreas.put("Area del Cargo:", new AreaConfig(52, 250, 12));
//            configuracionAreas.put("Funciones del Cargo:", new AreaConfig(70, 100, 12));
//            configuracionAreas.put("Profesión o Cargo:", new AreaConfig(70,  200, 12));
//
//            // ===== EXTRAER USANDO ÁREAS ESPECÍFICAS =====
//            PDFTextStripper stripper = new PDFTextStripper() {
//                @Override
//                protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
//                    for (TextPosition pos : textPositions) {
//                        for (Map.Entry<String, AreaConfig> entry : configuracionAreas.entrySet()) {
//                            String etiqueta = entry.getKey();
//                            AreaConfig config = entry.getValue();
//
//                            if (text.contains(etiqueta) && !campos.containsKey(etiqueta)) {
//                                float x = pos.getX();
//                                float y = pos.getY();
//
//                                Rectangle area = new Rectangle(
//                                        (int)(x + config.offsetX),
//                                        (int)(y - 10),
//                                        config.width,
//                                        config.height
//                                );
//
//                                PDFTextStripperByArea stripperByArea = new PDFTextStripperByArea();
//                                stripperByArea.setSortByPosition(true);
//                                stripperByArea.addRegion("valor", area);
//                                stripperByArea.extractRegions(getCurrentPage());
//
//                                String valor = stripperByArea.getTextForRegion("valor").trim();
//                                if (!valor.isEmpty()) {
//                                    campos.put(etiqueta, valor);
//                                }
//                            }
//                        }
//                    }
//                }
//            };
//            stripper.getText(document);
//
//            // ===== ASIGNAR A ENTIDAD (null si está vacío) =====
//            entity.setCityAndFetch(getValueOrNull(campos, "Ciudad y Fecha:"));
//            entity.setTypeOfMedicalEvaluationOrProcedure(getValueOrNull(campos, "Tipo Evaluación Médica o Procedimiento:"));
//            entity.setCompanyWhereYouWorkWillWorkOrWorked(getValueOrNull(campos, "Empresa Donde Labora, Laborará o Laboró:"));
//            entity.setNameOfWorkerToApplicant(getValueOrNull(campos, "Nombre del Trabajador a Aspirante:"));
//            entity.setGender(getValueOrNull(campos, "Genero:"));
//            entity.setAge(getValueOrNull(campos, "Edad:"));
//            entity.setMaritalStatus(getValueOrNull(campos, "Estado Civil:"));
//            entity.setEducationalLevel(getValueOrNull(campos, "Nivel Educativo:"));
//            entity.setEPS(getValueOrNull(campos, "E.P.S:"));
//            entity.setAFP(getValueOrNull(campos, "A.F.P:"));
//            entity.setARl(getValueOrNull(campos, "A.R.L:"));
//            entity.setBloodType(getValueOrNull(campos, "Tipo de Sangre:"));
//            entity.setNoChildren(getValueOrNull(campos, "N° Hijos"));
//            entity.setIncome(getValueOrNull(campos, "Ingresos:"));
//            entity.setWorkingHours(getValueOrNull(campos, "Jornada Laboral:"));
//            entity.setPositionArea(getValueOrNull(campos, "Area del Cargo:"));
//            entity.setProfessionOrPosition(getValueOrNull(campos, "Profesión o Cargo:"));
//            entity.setFunctionsOfThePosition(getValueOrNull(campos, "Funciones del Cargo:"));
//            entity.setEconomicActivity(getValueOrNull(campos, "Actividad Económica:"));
//            entity.setCC(getValueOrNull(campos, "CC:"));
//
//            extractPdfRepository.save(entity);
//            return ResponseEntity.ok(entity);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Error: " + e.getMessage());
//        }
//    }
//
//    // Clase auxiliar para configuración de áreas
//    private static class AreaConfig {
//        int offsetX;
//        int width;
//        int height;AreaConfig(int offsetX, int width, int height) {
//            this.offsetX = offsetX;
//            this.width = width;
//            this.height = height;
//        }
//    }
//
//    private String getValueOrNull(Map<String, String> map, String key) {
//        String value = map.get(key);
//        return (value == null || value.trim().isEmpty()) ? null : value;
//    }
//
//    private List<String> extraerImagenes(PDDocument document, String pdfFileName) {
//        List<String> rutasImagenes = new ArrayList<>();
//
//        try {
//            // Crear carpeta de imágenes si no existe
//            String currentDir = System.getProperty("user.dir");
//            Path imagePath = Paths.get(currentDir, IMAGES_FOLDER);
//
//            if (!Files.exists(imagePath)) {
//                Files.createDirectories(imagePath);
//            }
//
//            // Nombre base para las imágenes (sin extensión .pdf)
//            String baseFileName = pdfFileName.replaceAll("\\.pdf$", "");
//            int imageCounter = 1;
//
//            // Iterar sobre todas las páginas del documento
//            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
//                PDPage page = document.getPage(pageNum);
//                PDResources resources = page.getResources();
//
//                // Obtener todos los objetos XObject de la página
//                Iterable<COSName> xObjectNames = resources.getXObjectNames();
//
//                for (COSName name : xObjectNames) {
//                    PDXObject xObject = resources.getXObject(name);
//
//                    // Verificar si es una imagen
//                    if (xObject instanceof PDImageXObject) {
//                        PDImageXObject image = (PDImageXObject) xObject;
//
//                        // Generar nombre único para la imagen
//                        String imageName = String.format("%s_pagina%d_img%d.png",
//                                                         baseFileName,
//                                                         pageNum + 1,
//                                                         imageCounter);
//
//                        File imageFile = new File(imagePath.toFile(), imageName);
//
//                        // Guardar la imagen
//                        BufferedImage bImage = image.getImage();
//                        ImageIO.write(bImage, "png", imageFile);
//
//                        // Guardar la ruta relativa
//                        String relativePath = IMAGES_FOLDER + "/" + imageName;
//                        rutasImagenes.add(relativePath);
//
//                        imageCounter++;
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.err.println("Error al extraer imágenes: " + e.getMessage());
//        }
//
//        return rutasImagenes;
//    }
//
//    @PostMapping("/upload")
//    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return ResponseEntity.badRequest().body("Por favor selecciona un archivo");
//        }
//
//        String contentType = file.getContentType();
//        if (contentType == null || !contentType.equals("application/pdf")) {
//            return ResponseEntity.badRequest().body("Solo se permiten archivos PDF");
//        }
//
//        try {
//            String currentDir = System.getProperty("user.dir");
//            Path pdfPath = Paths.get(currentDir, PDF_FOLDER);
//
//            if (!Files.exists(pdfPath)) {
//                Files.createDirectories(pdfPath);
//            }
//
//            String fileName = file.getOriginalFilename();
//            Path destinationPath = pdfPath.resolve(fileName);
//            Files.copy(file.getInputStream(), destinationPath,
//                    REPLACE_EXISTING);
//
//            return ResponseEntity.ok("Archivo subido exitosamente en: " + destinationPath.toString());
//        } catch (IOException e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Error al guardar el archivo: " + e.getMessage());
//        }
//    }
//
//    @GetMapping("/all")
//    public ResponseEntity<List<Map<String, Object>>> getAllPdfs() {
//        List<ExtractPdf> pdfList = extractPdfRepository.findAll();
//        List<Map<String, Object>> response = new ArrayList<>();
//
//        for (ExtractPdf pdf : pdfList) {
//            Map<String, Object> map = new HashMap<>();
//            map.put("texto", pdf.getTexto());
//
//            // Agregar información de imágenes
//            if (pdf.getImagePaths() != null && !pdf.getImagePaths().isEmpty()) {
//                String[] paths = pdf.getImagePaths().split(",");
//                List<String> imagenes = Arrays.asList(paths);
//                map.put("imagenes", imagenes);
//                map.put("totalImagenes", imagenes.size());
//            } else {
//                map.put("imagenes", new ArrayList<>());
//                map.put("totalImagenes", 0);
//            }
//
//            response.add(map);
//        }
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/images/{fileName:.+}")
//    public ResponseEntity<?> getImage(@PathVariable String fileName) {
//        try {
//            String currentDir = System.getProperty("user.dir");
//            Path imagePath = Paths.get(currentDir, IMAGES_FOLDER, fileName);
//
//            if (!Files.exists(imagePath)) {
//                return ResponseEntity.notFound().build();
//            }
//
//            byte[] imageBytes = Files.readAllBytes(imagePath);
//
//            // Determinar el tipo de contenido
//            String contentType = "image/png";
//            if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
//                contentType = "image/jpeg";
//            }
//
//            return ResponseEntity.ok()
//                    .header("Content-Type", contentType)
//                    .body(imageBytes);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Error al cargar la imagen");
//        }
//    }
//}
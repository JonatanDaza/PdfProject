README - GUÍA DE INSTALACIÓN Y
ENTORNO
Sistema de Procesamiento y Lectura de Historias Clínicas (PDF + OCR)
Este documento proporciona las instrucciones completas para la configuración del entorno de desarrollo, la
instalación de dependencias externas y la puesta en marcha tanto del backend de Spring Boot como del
frontend de Next.js.
1. Arquitectura General y Stack Tecnológico
El sistema se compone de una arquitectura desacoplada estructurada en tres capas principales:
Backend: Spring Boot 3.2.4 con Java 17 , utilizando Apache PDFBox para manipulación de PDFs y
Tess4J (Tesseract) para la extracción de texto mediante OCR. Seguridad gestionada vía JWT.
Frontend: Next.js 16.2.10 con React 19 , TypeScript y Tailwind CSS para una interfaz responsive y ágil.
Base de Datos: PostgreSQL 14+ para la persistencia transaccional y el almacenamiento estructurado de
la información extraída.

2. Requisitos Previos e Instalación del Entorno
Antes de clonar e iniciar la aplicación, asegúrese de tener instalados los siguientes componentes de software
globales en su sistema operativo:
A. Java Development Kit (JDK) 17
Descargue e instale JDK 17 (se recomienda Eclipse Temurin o Oracle OpenJDK).
Configure la variable de entorno JAVA_HOME apuntando a la ruta de instalación.
Verifique la instalación ejecutando: java -version .
B. Node.js y Gestor de Paquetes (NPM)
Instale Node.js (versión LTS recomendada, v18 o superior).
Verifique la instalación ejecutando: node -v y npm -v .
C. Motor de OCR Tesseract
Dado que el backend procesa imágenes incrustadas mediante OCR utilizando la biblioteca Tess4J, es
obligatorio tener instalado el motor nativo en el host.
En Windows:
Descargue el instalador ejecutable de Tesseract OCR desde su repositorio oficial o fuentes confiables (ej.
UB Mannheim).
•

•
•

•
•
•

•
•

1.

Página 1 de 4

Durante la instalación, asegúrese de marcar la casilla para descargar los datos de idioma adicionales
( spa.traineddata para español).
Tome nota de la ruta de instalación por defecto. Comúnmente:
Instalación por usuario: C:\Users\<Tu-Usuario>\AppData\Local\Programs\Tesseract-OCR
Instalación global: C:\Program Files\Tesseract-OCR
IMPORTANTE PARA PRODUCCIÓN / LINUX:
Si despliega en un entorno basado en Linux, recuerde instalar el binario mediante el gestor de paquetes
correspondiente:

3. Configuración y Despliegue del Backend (Spring Boot)
A. Configuración de Base de Datos
Cree una base de datos vacía en PostgreSQL mediante su CLI o cliente de preferencia (pgAdmin):

B. Archivo de Propiedades de la Aplicación
Ubique el archivo src/main/resources/application.yml (o cree uno si no existe) y configure las
variables de entorno de la siguiente manera. Ajuste los marcadores según sus credenciales locales:
2.

3.
◦
◦

sudo apt-get install tesseract-ocr tesseract-ocr-spa

CREATE DATABASE DB_Name;

spring:
application:
name: NameProject
datasource:
url: jdbc:postgresql://localhost:5432/DB_Name
username: tu_usuario_postgres
password: tu_contraseña_postgres
driver-class-name: org.postgresql.Driver
jpa:
hibernate:
ddl-auto: update
open-in-view: false
show-sql: true
properties:
hibernate:
format_sql: true
jdbc:
lob:
non_contextual_creation: true
servlet:
multipart:
max-file-size: 50MB

Página 2 de 4

C. Compilación e Inicio del Servidor
Desde la carpeta raíz del proyecto backend donde se encuentra el archivo pom.xml , ejecute las siguientes
instrucciones utilizando el wrapper de Maven:
En Windows:

En macOS/Linux:

El servidor backend estará escuchando por defecto en el puerto configurado: http://localhost:8080 .
4. Configuración y Despliegue del Frontend (Next.js)
A. Variables de Entorno del Cliente
En la raíz del proyecto frontend, cree un archivo de configuración de entorno denominado .env.local e
ingrese la URL base del backend para comunicar la API de Axios:

B. Instalación de Dependencias e Inicio del Servidor de Desarrollo
Abra una nueva terminal en la carpeta raíz del módulo frontend y ejecute los siguientes comandos:
max-request-size: 50MB
server:
port: 8080
jwt:
secret: tu_clave_secreta_de_al_menos_256_bits_aqui_debe_ser_larga
expiration: 86400000
tesseract:
# Ruta a la carpeta que contiene los archivos *.traineddata
# Descomente la línea adecuada según su tipo de instalación:
# Opción Windows (instalación por usuario):
datapath: ${user.home}/AppData/Local/Programs/Tesseract-OCR/tessdata
# Opción Windows (instalación global):
# datapath: C:/Program Files/Tesseract-OCR/tessdata

mvnw.cmd clean install
mvnw.cmd spring-boot:run

chmod +x mvnw
./mvnw clean install
./mvnw spring-boot:run

NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# 1. Instalar todas las dependencias declaradas en el package.json
npm install

Página 3 de 4

Una vez iniciado con éxito, abra su navegador web e ingrese a la dirección del cliente: http://localhost:
3000 .
C. Scripts Disponibles en el Frontend
Comando Descripción y Propósito
npm run dev Inicia el servidor de desarrollo local en http://localhost:3000 .
npm run build Compila la aplicación optimizándola para producción. Genera páginas estáticas e híbridas.
npm run start Arranca el servidor de Next.js compilado en modo producción. Requiere un build previo.
npm run lint Ejecuta el análisis de código estático con ESLint para validar buenas prácticas y errores de

sintaxis.

5. Verificación del Funcionamiento Integrado
Para asegurar que todo el entorno se encuentra completamente acoplado y operativo, realice los siguientes
pasos:
Valide que la base de datos PostgreSQL haya creado automáticamente el esquema de tablas mediante el
ddl-auto: update de Hibernate al encender el backend.
Abra la aplicación frontend en el navegador y verifique en la consola del desarrollador (F12) que no
existan errores de bloqueo por políticas CORS al realizar peticiones HTTP hacia el backend.
Haga una prueba cargando un archivo PDF (de hasta 50MB permitidos según la directiva multipart )
para confirmar que Tesseract procesa correctamente el documento sin arrojar excepciones de falta de
librerías nativas en las rutas especificadas.
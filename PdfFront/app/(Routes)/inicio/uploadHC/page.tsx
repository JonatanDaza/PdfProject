"use client";

import { uploadHC, extractHC } from "@services/api/processPdfService";
import { useState } from "react";
import { toast } from "sonner";

type FileEntry = {
  id: number;
  file: File | null;
  status: "idle" | "processing" | "success" | "error";
  message?: string;
};

const UploadFile: React.FC = () => {
  const [files, setFiles] = useState<FileEntry[]>([
    { id: 1, file: null, status: "idle" },
  ]);
  const [processing, setProcessing] = useState(false);

  const addFile = () => {
    setFiles((currentFiles) => [
      ...currentFiles,
      {
        id: Date.now() + currentFiles.length,
        file: null,
        status: "idle",
      },
    ]);
  };

  const removeFile = (id: number) => {
    setFiles((currentFiles) => {
      if (currentFiles.length === 1) {
        return [{ id: 1, file: null, status: "idle" }];
      }

      const nextFiles = currentFiles.filter((entry) => entry.id !== id);

      return nextFiles.length > 0
        ? nextFiles
        : [{ id: 1, file: null, status: "idle" }];
    });
  };

  const handleFileChange = (id: number, e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0] ?? null;

    setFiles((currentFiles) =>
      currentFiles.map((entry) =>
        entry.id === id
          ? { ...entry, file: selectedFile, status: "idle", message: undefined }
          : entry
      )
    );
  };

  const handleCreateHistory = async () => {
    const selectedFiles = files.filter((entry) => entry.file);

    if (selectedFiles.length === 0) {
      toast.error("Selecciona al menos un archivo PDF");
      return;
    }

    setProcessing(true);

    try {
      for (const entry of selectedFiles) {
        if (!entry.file) {
          continue;
        }

        setFiles((currentFiles) =>
          currentFiles.map((fileEntry) =>
            fileEntry.id === entry.id
              ? { ...fileEntry, status: "processing", message: "Procesando..." }
              : fileEntry
          )
        );

        await uploadHC(entry.file);
        const extractResponse = await extractHC(entry.file);

        setFiles((currentFiles) =>
          currentFiles.map((fileEntry) =>
            fileEntry.id === entry.id
              ? {
                  ...fileEntry,
                  status: "success",
                  message: "Examen procesado correctamente",
                }
              : fileEntry
          )
        );

        toast.success(
          typeof extractResponse === "string"
            ? extractResponse
            : `Examen para ${entry.file.name} procesado correctamente`
        );
      }
    } catch (err: any) {
      const errorMessage = err?.message || "Error al procesar uno de los archivos";

      setFiles((currentFiles) =>
        currentFiles.map((fileEntry) =>
          fileEntry.status === "processing"
            ? { ...fileEntry, status: "error", message: errorMessage }
            : fileEntry
        )
      );

      toast.error(errorMessage);
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen w-full px-4 py-8 bg-white rounded-lg shadow-md border border-gray-200">
      <div className="p-8 w-full max-w-[60vw] max-h-[600vh] overflow-y-auto">
        <h2 className="text-2xl font-bold text-gray-800 mb-8 text-center">
          Crear Historia Clínica
        </h2>

        <div className="space-y-4 mb-6">
          {files.map((entry, index) => (
            <div
              key={entry.id}
              className="relative border-2 border-dashed border-blue-500 rounded-lg p-6 hover:border-blue-500 hover:bg-blue-50 transition-all bg-gray-50"
            >
              <button
                type="button"
                onClick={() => removeFile(entry.id)}
                disabled={processing}
                className="absolute right-0 top-0 inline-flex h-10 w-10 items-center justify-center rounded-full  text-blue-600 hover:text-red-600 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
                aria-label={`Eliminar archivo ${index + 1}`}
              >
                <span className="text-lg leading-none">×</span>
              </button>

              <label className="flex flex-col items-center justify-center cursor-pointer text-center gap-3">
                <svg
                  className="w-12 h-12 text-blue-500"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>

                <input
                  type="file"
                  onChange={(event) => handleFileChange(entry.id, event)}
                  accept=".pdf"
                  className="hidden"
                />

                <p className="text-sm font-semibold text-blue-600">
                  Archivo {index + 1}: {entry.file ? entry.file.name : "Selecciona un archivo PDF"}
                </p>

                {entry.message && (
                  <p
                    className={`text-xs font-medium ${
                      entry.status === "error"
                        ? "text-red-600"
                        : entry.status === "success"
                        ? "text-green-600"
                        : "text-gray-500"
                    }`}
                  >
                    {entry.message}
                  </p>
                )}
              </label>
            </div>
          ))}
        </div>

        <button
          onClick={addFile}
          disabled={processing}
          className="w-full bg-slate-700 hover:bg-slate-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 transform hover:scale-105 disabled:hover:scale-100 flex items-center justify-center gap-2"
        >
          Agregar otro archivo
        </button>

        <button
          onClick={handleCreateHistory}
          disabled={processing || files.every((entry) => !entry.file)}
          className="w-full mt-4 bg-green-500 hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 transform hover:scale-105 disabled:hover:scale-100 flex items-center justify-center gap-2"
        >
          {processing ? (
            <>
              <svg
                className="animate-spin h-5 w-5"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 
                  5.291A7.962 7.962 0 014 12H0c0 
                  3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              Creando historias...
            </>
          ) : (
            "Crear historia"
          )}
        </button>
      </div>
    </div>
  );
};

export default UploadFile;
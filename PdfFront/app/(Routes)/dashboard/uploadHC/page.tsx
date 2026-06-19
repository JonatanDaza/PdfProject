"use client";

import { uploadHC, extractHC } from "@services/api/processPdfService";
import { useState } from "react";

const UploadFile: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [extracting, setExtracting] = useState(false);
  const [extractResult, setExtractResult] = useState<any | null>(null);
  const [extractError, setExtractError] = useState<string | null>(null);

  // Captura el archivo seleccionado
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
      setResult(null);
      setError(null);
      setExtractResult(null);
      setExtractError(null);
    }
  };

  // Subida del archivo
  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    setError(null);

    try {
      const response = await uploadHC(file);
      setResult(response);
    } catch (err: any) {
      setError(err.message || "Error al subir el archivo");
    } finally {
      setUploading(false);
    }
  };

  // Extracción de la historia clínica
  const handleExtract = async () => {
    if (!file) return;
    setExtracting(true);
    setExtractError(null);

    try {
      const response = await extractHC(file);
      setExtractResult(response);
    } catch (err: any) {
      setExtractError(err.message || "Error al extraer la historia clínica");
    } finally {
      setExtracting(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen w-full px-4 py-8 bg-white rounded-lg shadow-md border border-gray-200">
      <div className="p-8 w-full max-w-md max-h-[90vh] overflow-y-auto">
        <h2 className="text-2xl font-bold text-gray-800 mb-8 text-center">
          Subir Historia Clínica
        </h2>

        <div className="border-2 border-dashed border-blue-500 rounded-lg p-8 mb-6 hover:border-blue-500 hover:bg-blue-50 transition-all cursor-pointer bg-gray-50">
          <label className="flex flex-col items-center justify-center cursor-pointer">
            <svg
              className="w-12 h-12 text-blue-500 mb-2"
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
              onChange={handleFileChange}
              accept=".pdf"
              className="hidden"
            />
            <p className="text-sm font-semibold text-blue-600 text-center">
              {file ? file.name : "Selecciona un archivo PDF"}
            </p>
          </label>
        </div>

        <button
          onClick={handleUpload}
          disabled={!file || uploading}
          className="w-full bg-green-400 hover:bg-blue-400 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 transform hover:scale-105 disabled:hover:scale-100 flex items-center justify-center gap-2"
        >
          {uploading ? (
            <>
              <svg className="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              Subiendo...
            </>
          ) : (
            "Subir archivo"
          )}
        </button>

        {result && (
          <div className="mt-4 p-4 bg-green-100 border-l-4 border-green-500 rounded-md">
            <p className="text-green-700 font-medium flex items-center gap-2">
              <span className="text-lg">✓</span>
              {result}
            </p>
          </div>
        )}

        {error && (
          <div className="mt-4 p-4 bg-red-100 border-l-4 border-red-500 rounded-md">
            <p className="text-red-700 font-medium flex items-center gap-2">
              <span className="text-lg">✕</span>
              {error}
            </p>
          </div>
        )}

        {/* Botón de extracción, visible una vez subido el archivo correctamente */}
        {result && (
          <button
            onClick={handleExtract}
            disabled={!file || extracting}
            className="w-full mt-4 bg-purple-500 hover:bg-purple-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 transform hover:scale-105 disabled:hover:scale-100 flex items-center justify-center gap-2"
          >
            {extracting ? (
              <>
                <svg className="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Extrayendo...
              </>
            ) : (
              "Extraer historia clínica"
            )}
          </button>
        )}

        {extractResult && (
          <div className="mt-4 p-4 bg-purple-100 border-l-4 border-purple-500 rounded-md">
            <p className="text-purple-700 font-medium mb-2 flex items-center gap-2">
              <span className="text-lg">✓</span>
              Extracción completada
            </p>
          </div>
        )}

        {extractError && (
          <div className="mt-4 p-4 bg-red-100 border-l-4 border-red-500 rounded-md">
            <p className="text-red-700 font-medium flex items-center gap-2">
              <span className="text-lg">✕</span>
              Extracción fallida
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default UploadFile;
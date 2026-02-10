"use client";

function HomeContent() {
  return (
      <main className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-bold">Lectura de Historias Clínicas</h1>
        <div className="mt-8">
          <button>
            <a href="/dashboard" className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
              Ir al Dashboard
            </a>
          </button>
        </div>
      </div>
    </main>
  );
}

export default function Home() {
  return (
    <HomeContent />
  );
}

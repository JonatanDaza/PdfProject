"use client";

import { useState } from "react";

export default function Home() {
  const [count, setCount] = useState(0);

  return (
    <main className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-bold">Lectura de Historias Clínicas</h1>
        <div className="mt-8">
          <button
            onClick={() => setCount((count) => count + 1)}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Count is {count}
          </button>
        </div>
      </div>
    </main>
  );
}

"use client";

import React from "react";

interface AudiometryRendererProps {
  data: any[];
}

const AudiometryRenderer: React.FC<AudiometryRendererProps> = ({ data }) => {
  const merged = Object.assign({}, ...data);

  const frequencies = Object.keys(merged)
    .map((key) => key.replace("VAOD", "").replace("VAOI", ""))
    .filter((value, index, self) => self.indexOf(value) === index)
    .sort((a, b) => Number(a) - Number(b));

  const vaodValues = frequencies.map((f) => merged[`VAOD${f}`] ?? "-");
  const vaoiValues = frequencies.map((f) => merged[`VAOI${f}`] ?? "-");

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="">
            <th className="text-left font-semibold">
              Frecuencia (Hz)
            </th>

            {frequencies.map((freq) => (
              <th key={freq} className="text-center font-semibold">
                {freq}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {/* VAOD */}
          <tr className="border-t border-gray-300">
            <td className="px-4 py-2 font-semibold text-blue-700">
              VAOD
            </td>

            {vaodValues.map((val, idx) => (
              <td key={idx} className="px-4 py-2 text-center">
                {val}
              </td>
            ))}
          </tr>

          {/* VAOI */}
          <tr className="border-t border-gray-300">
            <td className="px-4 py-2 font-semibold text-green-700">
              VAOI
            </td>

            {vaoiValues.map((val, idx) => (
              <td key={idx} className="px-4 py-2 text-center">
                {val}
              </td>
            ))}
          </tr>
        </tbody>
      </table>
    </div>
  );
};

export default AudiometryRenderer;
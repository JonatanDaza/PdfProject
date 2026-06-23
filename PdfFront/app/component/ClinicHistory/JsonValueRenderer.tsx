"use client";

import React from "react";
import AudiometryRenderer from "./AudiometryRenderer";

interface JsonValueRendererProps {
  value: string;
}

// 🔥 Keys que nunca quieres mostrar
const HIDDEN_KEYS = [
  "recommendationConductText",
  "recommendationText",
  "observationText",
  "conductText",
];

const isAudiometry = (data: any[]) => {
  return data.some((row) =>
    Object.keys(row).some(
      (key) => key.startsWith("VAOD") || key.startsWith("VAOI")
    )
  );
};

const isRequirementList = (data: any[]) => {
  return data.every(
    (item) =>
      typeof item === "object" &&
      "requisito" in item &&
      "cumple" in item
  );
};

// 👉 Texto único tipo {"recommendationText": "..."}
const isSingleText = (data: any[]) => {
  return (
    data.length === 1 &&
    typeof data[0] === "object" &&
    Object.keys(data[0]).length === 1
  );
};

const JsonValueRenderer: React.FC<JsonValueRendererProps> = ({ value }) => {
  try {
    const parsed = JSON.parse(value);

    if (!Array.isArray(parsed)) {
      return (
        <pre className="text-sm whitespace-pre-wrap">
          {JSON.stringify(parsed, null, 2)}
        </pre>
      );
    }

    if (isAudiometry(parsed)) {
      return <AudiometryRenderer data={parsed} />;
    }

    if (isSingleText(parsed)) {
      const obj = parsed[0];
      const key = Object.keys(obj)[0];
      const cleanValue = obj[key];

      return (
        <div className="">
          <p className="text-sm text-gray-800 whitespace-pre-wrap">
            {cleanValue}
          </p>
        </div>
      );
    }

    if (isRequirementList(parsed)) {
      return (
        <div className="space-y-2">
          {parsed.map((item, index) => (
            <div
              key={index}
              className="flex items-start justify-between"
            >
              <span className="text-sm text-gray-700">
                {item.requisito}
              </span>

              <span
                className= "px-10 py-1 text-xs font-semibold border-l"
              >
                {item.cumple}
              </span>
            </div>
          ))}
        </div>
      );
    }

    const keys = [
      ...new Set(
        parsed.flatMap((obj: any) =>
          Object.keys(obj).filter(
            (k) => !HIDDEN_KEYS.includes(k)
          )
        )
      ),
    ];

    return (
      <div className="overflow-x-auto">
        <table className="w-full min-w-max">
          <thead>
            <tr>
              {keys.map((key) => (
                <th
                  key={key}
                  className="text-left font-semibold"
                >
                  {key}
                </th>
              ))}
            </tr>
          </thead>

          <tbody>
            {parsed.map((row, index) => (
              <tr key={index}>
                {keys.map((key) => (
                  <td key={key} className="">
                    {row[key] ?? "-"}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  } catch {
    return (
      <div className="whitespace-pre-wrap text-sm">
        {value}
      </div>
    );
  }
};

export default JsonValueRenderer;
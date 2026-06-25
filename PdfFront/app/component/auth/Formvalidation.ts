// Refleja exactamente las anotaciones de validación del backend

export interface ValidationRule {
  validate: (value: string) => boolean;
  message: string;
}

export type FieldValidations = Record<string, ValidationRule[]>;

export interface PasswordHint {
  label: string;
  met: (value: string) => boolean;
}

export const passwordHints: PasswordHint[] = [
  {
    label: "Al menos 8 caracteres",
    met: (v) => v.length >= 8,
  },
  {
    label: "Una letra mayúscula",
    met: (v) => /[A-Z]/.test(v),
  },
  {
    label: "Una letra minúscula",
    met: (v) => /[a-z]/.test(v),
  },
  {
    label: "Un número",
    met: (v) => /[0-9]/.test(v),
  },
  {
    label: "Un símbolo (!@#$...)",
    met: (v) => /[^A-Za-z0-9]/.test(v),
  },
];

// Devuelve un nivel 0-4 según cuántas sugerencias se cumplen (excluye la de longitud mínima del backend)
export type StrengthLevel = 0 | 1 | 2 | 3 | 4;

export const getPasswordStrength = (value: string): StrengthLevel => {
  if (!value) return 0;
  const score = passwordHints.filter((h) => h.met(value)).length;
  if (score <= 1) return 1;
  if (score === 2) return 2;
  if (score === 3 || score === 4) return 3;
  return 4;
};

export const strengthConfig: Record<
  StrengthLevel,
  { label: string; color: string; bars: number }
> = {
  0: { label: "",          color: "bg-gray-600",  bars: 0 },
  1: { label: "Muy débil", color: "bg-red-500",   bars: 1 },
  2: { label: "Débil",     color: "bg-orange-400", bars: 2 },
  3: { label: "Aceptable", color: "bg-yellow-400", bars: 3 },
  4: { label: "Fuerte",    color: "bg-green-500",  bars: 4 },
};

export const loginValidations: FieldValidations = {
  email: [
    {
      validate: (v) => v.trim().length > 0,
      message: "El email es obligatorio",
    },
    {
      validate: (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),
      message: "Formato de email inválido",
    },
  ],
  password: [
    {
      validate: (v) => v.trim().length > 0,
      message: "La contraseña es obligatoria",
    },
  ],
};

export const registerValidations: FieldValidations = {
  name: [
    {
      validate: (v) => v.trim().length > 0,
      message: "El nombre es obligatorio",
    },
    {
      validate: (v) => v.trim().length >= 2 && v.trim().length <= 100,
      message: "El nombre debe tener entre 2 y 100 caracteres",
    },
  ],
  email: [
    {
      validate: (v) => v.trim().length > 0,
      message: "El email es obligatorio",
    },
    {
      validate: (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),
      message: "Formato de email inválido",
    },
  ],
  password: [
    {
      validate: (v) => v.trim().length > 0,
      message: "La contraseña es obligatoria",
    },
    {
      validate: (v) => v.length >= 8,
      message: "La contraseña debe tener al menos 8 caracteres",
    },
  ],
};

// Helper: valida un campo y devuelve el primer error encontrado o null
export const validateField = (
  fieldId: string,
  value: string,
  validations: FieldValidations
): string | null => {
  const rules = validations[fieldId];
  if (!rules) return null;
  for (const rule of rules) {
    if (!rule.validate(value)) return rule.message;
  }
  return null;
};

// Helper: valida todos los campos y devuelve true si el formulario es válido
export const isFormValid = (
  values: Record<string, string>,
  validations: FieldValidations
): boolean => {
  return Object.keys(validations).every((fieldId) =>
    validateField(fieldId, values[fieldId] ?? "", validations) === null
  );
};
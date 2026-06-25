import { API } from "@/services/api/api";
import { RoleDto } from "@/types/api";

const login = async (email: string, password: string) => {
  const response = await fetch(`${API.API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || 'Error en la autenticación');
  }

  const userDataFromResponse = data.data || data;
  const tokenFromResponse = userDataFromResponse.token || data.token;

  if (tokenFromResponse) {
    const role = userDataFromResponse.role || 'USER';

    const userData = {
      id: userDataFromResponse.id,
      name: userDataFromResponse.name || '',
      email: userDataFromResponse.email,
      role,
      token: tokenFromResponse,
      ...userDataFromResponse,
    };

    localStorage.setItem('token', tokenFromResponse);
    localStorage.setItem('userData', JSON.stringify(userData));

    // Cookies para el middleware
    document.cookie = `token=${tokenFromResponse}; path=/; max-age=86400`;
    document.cookie = `userRole=${role}; path=/; max-age=86400`;
  }

  return data;
};

const register = async (name: string, email: string, password: string) => {
  const response = await fetch(`${API.API_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password }), // nunca enviamos roles aquí
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || 'Error al registrar el usuario');
  }

  return data;
};

// Registro administrativo: requiere token de ADMIN, puede elegir roles
const adminCreateUser = async (
  name: string,
  email: string,
  password: string,
  roles?: string[]
) => {
  const token = localStorage.getItem('token');

  if (!token) {
    throw new Error('No hay sesión activa. Inicia sesión como administrador.');
  }

  const response = await fetch(`${API.API_BASE_URL}/auth/admin/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({
      name,
      email,
      password,
      ...(roles && roles.length > 0 ? { roles } : {}),
    }),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || 'Error al crear el usuario');
  }

  return data;
};

const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('userData');
  document.cookie = 'token=; path=/; max-age=0';
  document.cookie = 'userRole=; path=/; max-age=0';
};

const changePassword = async (userId: number, newPassword: string) => {
  const token = localStorage.getItem('token');
  if (!token) throw new Error('No hay sesión activa. Inicia sesión como administrador.');

  const response = await fetch(`${API.API_BASE_URL}/auth/${userId}/password`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ newPassword }),
  });

  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Error al cambiar la contraseña');
  return data;
};

const getAllUsers = async () => {
  const token = localStorage.getItem('token');
  if (!token) throw new Error('No hay sesión activa.');
  const response = await fetch(`${API.API_BASE_URL}/auth/users`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Error al obtener usuarios');
  return data;
};

const getUserById = async (userId: number) => {
  const token = localStorage.getItem('token');
  if (!token) throw new Error('No hay sesión activa.');
  const response = await fetch(`${API.API_BASE_URL}/auth/users/${userId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Error al obtener usuario');
  return data;
};

const getRedirectUrl = (): string => {
  try {
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
    const userRole = userData?.role || userData?.roles?.[0]?.name || userData?.roles?.[0] || '';
    return String(userRole).toUpperCase() === 'USER' ? '/inicio' : '/dashboard';
  } catch {
    return '/dashboard';
  }
};

const changeRole = async (userId: number, newRoleId: number) => {
  const token = localStorage.getItem('token');

  if (!token) {
    throw new Error('No hay sesión activa. Inicia sesión como administrador.');
  }

  const response = await fetch(`${API.API_BASE_URL}/auth/users/${userId}/role/${newRoleId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || 'Error al cambiar el rol del usuario');
  }

  return data;
};

const getRoles = async (): Promise<RoleDto[]> => {
  const token = localStorage.getItem('token');
  if (!token) throw new Error('No hay sesión activa.');

  const response = await fetch(`${API.API_BASE_URL}/auth/roles`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });

  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Error al obtener roles');
  return data.data ?? [];
};

const deactivateUser = async (userId: number) => {
  const token = localStorage.getItem('token');
  if (!token) throw new Error('No hay sesión activa.');
  const response = await fetch(`${API.API_BASE_URL}/auth/users/${userId}/deactivate`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Error al desactivar usuario');
  return data;
};

const activateUser = async (userId: number) => {
  const token = localStorage.getItem('token');
  if (!token) throw new Error('No hay sesión activa.');
  const response = await fetch(`${API.API_BASE_URL}/auth/users/${userId}/activate`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Error al activar usuario');
  return data;
};

export const authService = {
  login,
  register,
  adminCreateUser,
  logout,
  changePassword,
  getAllUsers,
  getUserById,
  getRedirectUrl,
  changeRole,
  getRoles,
  deactivateUser,
  activateUser,
};
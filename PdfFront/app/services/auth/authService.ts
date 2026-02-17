import { API } from "@/services/api/api";

const login = async (email: string, password: string) => {
  const response = await fetch(`${API.API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ email, password })
  });
  
  const data = await response.json();
  
  if (!response.ok) {
    throw new Error(data.message || 'Error en la autenticación');
  }

  // Guardar token y datos de usuario
  // La respuesta tiene estructura: {date, code, data: {...}, message}
  const userDataFromResponse = data.data || data;
  const tokenFromResponse = userDataFromResponse.token || data.token;
  
  if (tokenFromResponse) {
    localStorage.setItem('token', tokenFromResponse);
    
    // Asegurar que userData tiene la estructura correcta
    const userData = {
      id: userDataFromResponse.id,
      name: userDataFromResponse.name || '',
      email: userDataFromResponse.email,
      role: userDataFromResponse.role || 'USER',
      token: tokenFromResponse,
      ...userDataFromResponse
    };
    localStorage.setItem('userData', JSON.stringify(userData));
    
    // También guardar token en cookie
    document.cookie = `token=${tokenFromResponse}; path=/; max-age=86400`;
  }
  
  return data;
};

const register = async (name: string, email: string, password: string) => {
  const response = await fetch(`${API.API_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ name, email, password })
  });
  
  const data = await response.json();
  
  if (!response.ok) {
    throw new Error(data.message || 'Error al registrarse');
  }

  return data;
};

const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('userData');
  document.cookie = 'token=; path=/; max-age=0';
};

const getRedirectUrl = () => {
  try {
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
    return '/dashboard';
  } catch {
    return '/dashboard';
  }
};

export const authService = {
  login,
  register,
  logout,
  getRedirectUrl
};
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

  console.log("[authService] Respuesta del servidor:", data);

  // Guardar token y datos de usuario
  // La respuesta tiene estructura: {date, code, data: {...}, message}
  const userDataFromResponse = data.data || data;
  const tokenFromResponse = userDataFromResponse.token || data.token;
  
  if (tokenFromResponse) {
    console.log("[authService] Token a guardar:", tokenFromResponse);
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
    
    console.log("[authService] Guardando userData:", userData);
    localStorage.setItem('userData', JSON.stringify(userData));
    
    // También guardar token en cookie
    document.cookie = `token=${tokenFromResponse}; path=/; max-age=86400`;
  }
  
  return data;
};

const logout = () => {
  console.log("[authService] Cerrando sesión");
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
  logout,
  getRedirectUrl
};
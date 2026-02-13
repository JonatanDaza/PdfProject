const HOST = process.env.NEXT_PUBLIC_API_BASE_URL;

const login = async (email: string, password: string) => {
  const response = await fetch(`${HOST}/api/auth/login`, {
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
  
  return data;
};

const getRedirectUrl = () => {
  try {
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
    return userData.role === 'ADMIN' ? '/admin/dashboard' : '/';
  } catch {
    return '/';
  }
};

export const authService = {
  login,
  getRedirectUrl
};
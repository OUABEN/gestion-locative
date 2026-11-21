import axios from 'axios';

export const authApi = {
  login: async (email, password) => {
    const response = await axios.post('/api/auth/login', { email, password });
    return response.data;
  },

  register: async ({ nom, prenom, email, password, role }) => {
    const response = await axios.post('/api/auth/register', {
      nom,
      prenom,
      email,
      password,
      role,
    });
    return response.data;
  },
};

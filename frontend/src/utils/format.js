// Formatage en Dirham marocain "4 500 DH" (Intl avec fr-MA)
export const formatCurrency = (amount) => {
  if (amount === undefined || amount === null || isNaN(amount)) return '0 DH';
  const formatted = new Intl.NumberFormat('fr-MA', {
    maximumFractionDigits: 0,
  }).format(amount);
  return `${formatted} DH`;
};

// Formatage surface "75 m²"
export const formatSurface = (surface) => {
  if (surface === undefined || surface === null || isNaN(surface)) return '0 m²';
  return `${surface} m²`;
};

// Formatage date "jj/mm/aaaa"
export const formatDate = (dateString) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
};


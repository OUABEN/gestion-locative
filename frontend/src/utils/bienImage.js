// Images locales des biens (aucune ressource externe).
// import.meta.glob est tolérant : si un fichier manque, un visuel de repli est utilisé.
const modules = import.meta.glob('../assets/biens/*.{jpg,jpeg,png,webp}', {
  eager: true,
  import: 'default',
});

const images = {};
Object.entries(modules).forEach(([path, url]) => {
  const name = path.split('/').pop().replace(/\.(jpg|jpeg|png|webp)$/i, '');
  images[name] = url;
});

const normalize = (str = '') =>
  str
    .toString()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');

// Visuel de repli (SVG inline aux couleurs de la charte)
const FALLBACK =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500">
      <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#E6D3B3"/><stop offset="1" stop-color="#CBB28D"/>
      </linearGradient></defs>
      <rect width="800" height="500" fill="url(#g)"/>
      <path d="M250 380V230l150-110 150 110v150z" fill="#6B4426"/>
      <rect x="370" y="290" width="60" height="90" fill="#F6EFE2"/>
      <circle cx="610" cy="360" r="34" fill="#2E7D4F"/>
    </svg>`
  );

/** Retourne l'URL d'image correspondant au type du bien. */
export const getBienImage = (type) => {
  const t = normalize(type);
  let key = 'appartement';
  if (t.includes('villa')) key = 'villa';
  else if (t.includes('studio')) key = 'studio';
  else if (t.includes('bureau')) key = 'bureau';
  else if (t.includes('local') || t.includes('commerc') || t.includes('boutique')) key = 'local-commercial';
  else if (t.includes('maison') || t.includes('riad')) key = 'maison';

  // Si l'image dédiée n'existe pas encore, on utilise la plus proche disponible.
  const alternatives = {
    bureau: ['studio', 'appartement'],
    'local-commercial': ['appartement', 'studio'],
    maison: ['villa', 'appartement'],
  };
  const chain = [key, ...(alternatives[key] || []), 'appartement'];
  const found = chain.find((k) => images[k]);
  return found ? images[found] : FALLBACK;
};

export const getHeroImage = () => images.villa || images.appartement || FALLBACK;

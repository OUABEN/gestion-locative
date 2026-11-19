import { z } from 'zod';

export const bienSchema = z.object({
  adresse: z.string().trim().min(1, "L'adresse est obligatoire."),
  ville: z.string().trim().min(1, 'La ville est obligatoire.'),
  type: z.string().trim().min(1, 'Le type de bien est obligatoire.'),
  surface: z
    .number({ invalid_type_error: 'La surface doit être un nombre.' })
    .positive('La surface doit être strictement supérieure à 0 m².'),
  prixMensuel: z
    .number({ invalid_type_error: 'Le loyer doit être un nombre.' })
    .min(0, 'Le loyer mensuel ne peut pas être négatif.'),
  charges: z
    .number({ invalid_type_error: 'Les charges doivent être un nombre.' })
    .min(0, 'Les charges ne peuvent pas être négatives.')
    .default(0),
  statut: z.enum(['LIBRE', 'OCCUPE']).optional(),
  imageUrl: z.string().optional().nullable(),
});

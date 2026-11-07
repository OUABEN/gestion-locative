import jwt from "jsonwebtoken";
import dotenv from "dotenv";
import path from "path";

// Tente de charger le .env à la racine ou local
dotenv.config({ path: path.resolve(__dirname, "../../.env") });
dotenv.config();

const secret = process.env.JWT_SECRET || "change_me_with_a_long_random_secret_of_at_least_32_chars";
const role = process.argv[2] || "BAILLEUR";
const sub = process.argv[3] || "11111111-1111-1111-1111-111111111111";

const token = jwt.sign(
  {
    sub,
    role,
    email: `${role.toLowerCase()}@gestion-locative.com`,
  },
  secret,
  { expiresIn: "24h" }
);

console.log(`\n=== Jeton JWT généré (${role}) ===\n`);
console.log(token);
console.log(`\nExemple curl :`);
console.log(`curl -H "Authorization: Bearer ${token}" http://localhost:8081/api/biens\n`);


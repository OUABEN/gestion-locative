import { Router } from "express";
import { AuthController } from "../controllers/auth.controller";
import { authenticate, requireRole } from "../middlewares/auth.middleware";
import { validateUuidParam } from "../middlewares/validation.middleware";

export const authRouter = Router();
export const userRouter = Router();
const controller = new AuthController();

authRouter.post("/register", controller.register);
authRouter.post("/login", controller.login);
authRouter.get("/me", authenticate, controller.me);

userRouter.get(
  "/:id",
  authenticate,
  requireRole("BAILLEUR", "GESTIONNAIRE"),
  validateUuidParam("id"),
  controller.getUserById
);

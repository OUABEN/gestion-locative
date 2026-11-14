class CinDejaUtiliseException extends Error {
  constructor(message = 'Fiche refusée : CIN déjà utilisé') {
    super(message);
    this.name = 'CinDejaUtiliseException';
    this.statusCode = 409;
  }
}

module.exports = CinDejaUtiliseException;

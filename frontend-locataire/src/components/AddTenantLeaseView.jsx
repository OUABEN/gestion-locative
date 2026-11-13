import React, { useState } from 'react';
import { 
  UserPlus, 
  Building2, 
  Coins, 
  UploadCloud, 
  FileText, 
  Trash2, 
  AlertTriangle, 
  CheckCircle2, 
  ShieldCheck, 
  Calendar,
  Phone,
  Mail
} from 'lucide-react';

export default function AddTenantLeaseView({ onSaveNewLease, existingCins, onCancel }) {
  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    cin: '',
    telephone: '',
    email: '',
    bienTitre: 'Appartement B-14 (Quartier Racine)',
    bienAdresse: '78, Rue Jean Jaurès, Gauthier',
    bienVille: 'Casablanca',
    bienType: 'Appartement F4 Standing',
    loyerMensuel: 6500,
    depotGarantie: 13000,
    dateDebut: '01/01/2026',
    dateFin: '31/12/2027',
    statutBail: 'ACTIF'
  });

  const [uploadedFiles, setUploadedFiles] = useState([
    { nom: 'CIN_RectoVerso_Scanne.pdf', taille: '1.2 Mo' },
    { nom: 'Justificatif_Domicile_Attestation.pdf', taille: '890 Ko' }
  ]);

  const [errorCin, setErrorCin] = useState('');

  // Vérification de l'unicité du CIN en temps réel
  const handleCinChange = (e) => {
    const rawVal = e.target.value;
    const normalized = rawVal.trim().toUpperCase();
    setFormData({ ...formData, cin: rawVal });

    if (normalized.length >= 4) {
      if (existingCins.includes(normalized)) {
        setErrorCin(`Fiche refusée : CIN déjà utilisé (${normalized})`);
      } else {
        setErrorCin('');
      }
    } else {
      setErrorCin('');
    }
  };

  const handleFileUpload = (e) => {
    const files = Array.from(e.target.files);
    const newFiles = files.map((file) => ({
      nom: file.name,
      taille: `${(file.size / (1024 * 1024)).toFixed(1)} Mo`
    }));
    setUploadedFiles([...uploadedFiles, ...newFiles]);
  };

  const removeFile = (idx) => {
    setUploadedFiles(uploadedFiles.filter((_, i) => i !== idx));
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    const normalizedCin = formData.cin.trim().toUpperCase();
    if (existingCins.includes(normalizedCin)) {
      setErrorCin(`Fiche refusée : CIN déjà utilisé (${normalizedCin})`);
      return;
    }

    if (!formData.nom || !formData.prenom || !formData.cin || !formData.loyerMensuel) {
      alert("Veuillez renseigner tous les champs obligatoires (*).");
      return;
    }

    onSaveNewLease({
      id: `loc-${Date.now().toString(36)}`,
      nom: formData.nom.trim(),
      prenom: formData.prenom.trim(),
      cin: normalizedCin,
      telephone: formData.telephone.trim(),
      email: formData.email.trim(),
      bien: {
        titre: formData.bienTitre,
        adresse: formData.bienAdresse,
        ville: formData.bienVille,
        type: formData.bienType
      },
      loyerMensuel: Number(formData.loyerMensuel),
      depotGarantie: Number(formData.depotGarantie),
      dateDebut: formData.dateDebut,
      dateFin: formData.dateFin,
      statutBail: formData.statutBail,
      quittancesEmises: 0,
      piecesJustificatives: uploadedFiles
    });
  };

  const isDuplicate = Boolean(errorCin);

  return (
    <div className="rounded-card bg-white border border-slate-200/90 shadow-card-soft p-6 sm:p-8 select-none">
      
      {/* Form Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 mb-6 border-b border-slate-200 gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <span className="w-2.5 h-2.5 rounded-full bg-lime shadow-lime-sm"></span>
            <span className="text-[11px] font-mono font-black text-slate-500 uppercase tracking-wider">
              Enregistrement Officiel
            </span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-midnight tracking-tight mt-1">
            Création d'un Nouveau Locataire & Bail Rattaché
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Génération automatique de l'UUID, contrôle d'unicité absolue du CIN et préparation du flux quittances.
          </p>
        </div>

        <button
          onClick={onCancel}
          className="self-start sm:self-auto px-4 py-2 rounded-button bg-slate-100 hover:bg-slate-200 text-xs font-extrabold text-slate-700 transition-colors"
        >
          Retour au répertoire
        </button>
      </div>

      {/* Alerte Erreur HTTP 409 si CIN déjà utilisé */}
      {errorCin && (
        <div className="mb-6 p-4 rounded-xl bg-coral-50 border-2 border-coral text-coral-600 flex items-start space-x-3.5 animate-in fade-in">
          <AlertTriangle className="w-5 h-5 text-coral flex-shrink-0 mt-0.5" />
          <div className="text-xs">
            <span className="font-black text-sm text-coral-600 block">
              {errorCin}
            </span>
            <p className="text-slate-700 mt-1">
              Erreur d'intégrité <strong>HTTP 409 Conflict</strong> (Règle UML Sujet 16). Le CIN est une clé métier strictement unique dans le système. Veuillez vérifier le dossier du candidat.
            </p>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-8">
        
        {/* SECTION 1 : Identité du Locataire */}
        <div>
          <div className="flex items-center space-x-2 mb-4">
            <div className="w-7 h-7 rounded-lg bg-midnight text-lime flex items-center justify-center font-bold text-xs">
              1
            </div>
            <h3 className="text-sm font-extrabold text-midnight uppercase tracking-wider">
              Identité & Coordonnées du Locataire
            </h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 text-xs">
            {/* Nom */}
            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">
                Nom de famille <span className="text-coral">*</span>
              </label>
              <input
                type="text"
                required
                placeholder="ex. Benjelloun"
                value={formData.nom}
                onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-button focus:outline-none focus:ring-2 focus:ring-midnight focus:bg-white transition-all text-xs"
              />
            </div>

            {/* Prénom */}
            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">
                Prénom <span className="text-coral">*</span>
              </label>
              <input
                type="text"
                required
                placeholder="ex. Mehdi"
                value={formData.prenom}
                onChange={(e) => setFormData({ ...formData, prenom: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-button focus:outline-none focus:ring-2 focus:ring-midnight focus:bg-white transition-all text-xs"
              />
            </div>

            {/* CIN Marocain */}
            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="block font-bold text-slate-700 uppercase">
                  CIN Marocain <span className="text-coral">*</span> [Unique]
                </label>
                {formData.cin.trim().length >= 4 && !errorCin && (
                  <span className="text-[10px] font-bold text-emerald-600 flex items-center">
                    <CheckCircle2 className="w-3 h-3 mr-0.5" />
                    Valide
                  </span>
                )}
              </div>
              <input
                type="text"
                required
                placeholder="ex. EF345678 (Normalisé)"
                value={formData.cin}
                onChange={handleCinChange}
                className={`w-full px-3.5 py-2.5 font-mono rounded-button focus:outline-none transition-all text-xs ${
                  isDuplicate
                    ? 'border-2 border-coral bg-coral-50/50 text-coral-600 focus:ring-2 focus:ring-coral'
                    : 'border border-slate-200 bg-slate-50 focus:ring-2 focus:ring-midnight focus:bg-white'
                }`}
              />
              <p className="text-[10px] text-slate-400 mt-1">
                Test doublon : <code className="bg-slate-100 font-bold px-1 rounded cursor-pointer" onClick={() => handleCinChange({ target: { value: 'AB123456' } })}>AB123456</code>
              </p>
            </div>

            {/* Téléphone */}
            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">
                Téléphone marocain <span className="text-coral">*</span>
              </label>
              <input
                type="tel"
                required
                placeholder="+212 6 XX XX XX XX"
                value={formData.telephone}
                onChange={(e) => setFormData({ ...formData, telephone: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-button focus:outline-none focus:ring-2 focus:ring-midnight focus:bg-white transition-all text-xs font-mono"
              />
            </div>

            {/* Email */}
            <div className="sm:col-span-2">
              <label className="block font-bold text-slate-700 uppercase mb-1">
                Adresse Email <span className="text-coral">*</span>
              </label>
              <input
                type="email"
                required
                placeholder="locataire@exemple.ma"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-button focus:outline-none focus:ring-2 focus:ring-midnight focus:bg-white transition-all text-xs"
              />
            </div>
          </div>
        </div>

        {/* SECTION 2 : Bien Immobilier Rattaché */}
        <div className="pt-4 border-t border-slate-200">
          <div className="flex items-center space-x-2 mb-4">
            <div className="w-7 h-7 rounded-lg bg-midnight text-lime flex items-center justify-center font-bold text-xs">
              2
            </div>
            <h3 className="text-sm font-extrabold text-midnight uppercase tracking-wider">
              Bien Immobilier Rattaché (Module Biens - Mohamed)
            </h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">Titre du bien</label>
              <input
                type="text"
                value={formData.bienTitre}
                onChange={(e) => setFormData({ ...formData, bienTitre: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-button text-xs font-semibold"
              />
            </div>
            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">Adresse & Quartier</label>
              <input
                type="text"
                value={formData.bienAdresse}
                onChange={(e) => setFormData({ ...formData, bienAdresse: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-button text-xs font-semibold"
              />
            </div>
            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">Ville</label>
              <select
                value={formData.bienVille}
                onChange={(e) => setFormData({ ...formData, bienVille: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-button text-xs font-semibold"
              >
                <option value="Casablanca">Casablanca</option>
                <option value="Rabat">Rabat</option>
                <option value="Marrakech">Marrakech</option>
                <option value="Tanger">Tanger</option>
              </select>
            </div>
          </div>
        </div>

        {/* SECTION 3 : Paramètres Financiers du Bail */}
        <div className="pt-4 border-t border-slate-200">
          <div className="flex items-center space-x-2 mb-4">
            <div className="w-7 h-7 rounded-lg bg-midnight text-lime flex items-center justify-center font-bold text-xs">
              3
            </div>
            <h3 className="text-sm font-extrabold text-midnight uppercase tracking-wider">
              Modalités Financières & Dates d'Effet (DH)
            </h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">
                Loyer Mensuel (DH) <span className="text-coral">*</span>
              </label>
              <input
                type="number"
                required
                min="500"
                step="50"
                value={formData.loyerMensuel}
                onChange={(e) => setFormData({ ...formData, loyerMensuel: e.target.value })}
                className="w-full px-3.5 py-2.5 font-mono font-bold bg-slate-50 border border-slate-200 rounded-button text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">
                Dépôt de Garantie (DH) <span className="text-coral">*</span>
              </label>
              <input
                type="number"
                required
                min="0"
                step="100"
                value={formData.depotGarantie}
                onChange={(e) => setFormData({ ...formData, depotGarantie: e.target.value })}
                className="w-full px-3.5 py-2.5 font-mono font-bold bg-slate-50 border border-slate-200 rounded-button text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">
                Date de début du bail <span className="text-coral">*</span>
              </label>
              <input
                type="text"
                placeholder="01/01/2026"
                value={formData.dateDebut}
                onChange={(e) => setFormData({ ...formData, dateDebut: e.target.value })}
                className="w-full px-3.5 py-2.5 font-mono bg-slate-50 border border-slate-200 rounded-button text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 uppercase mb-1">
                Date de fin prévue <span className="text-coral">*</span>
              </label>
              <input
                type="text"
                placeholder="31/12/2027"
                value={formData.dateFin}
                onChange={(e) => setFormData({ ...formData, dateFin: e.target.value })}
                className="w-full px-3.5 py-2.5 font-mono bg-slate-50 border border-slate-200 rounded-button text-xs"
              />
            </div>
          </div>
        </div>

        {/* SECTION 4 : Pièces Justificatives */}
        <div className="pt-4 border-t border-slate-200">
          <div className="flex items-center space-x-2 mb-3">
            <div className="w-7 h-7 rounded-lg bg-midnight text-lime flex items-center justify-center font-bold text-xs">
              4
            </div>
            <h3 className="text-sm font-extrabold text-midnight uppercase tracking-wider">
              Dossier Documentaire (Pièces Justificatives Privées)
            </h3>
          </div>

          <div className="border-2 border-dashed border-slate-200 hover:border-midnight rounded-card p-6 text-center bg-slate-50/50 transition-colors relative cursor-pointer">
            <input
              type="file"
              multiple
              onChange={handleFileUpload}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            />
            <UploadCloud className="w-8 h-8 text-midnight mx-auto mb-2" />
            <p className="text-xs font-bold text-midnight">
              Glissez-déposez la CIN scannée, attestation de travail ou contrat
            </p>
            <p className="text-[11px] text-slate-400 mt-0.5">Formats certifiés : PDF, JPEG (Max 10 Mo par document)</p>
          </div>

          {uploadedFiles.length > 0 && (
            <div className="mt-3 space-y-1.5">
              {uploadedFiles.map((f, idx) => (
                <div key={idx} className="flex items-center justify-between p-2.5 bg-slate-100 rounded-lg text-xs font-mono">
                  <div className="flex items-center space-x-2 truncate">
                    <FileText className="w-4 h-4 text-midnight flex-shrink-0" />
                    <span className="font-semibold text-slate-800 truncate">{f.nom}</span>
                    <span className="text-slate-400 text-[10px]">({f.taille})</span>
                  </div>
                  <button type="button" onClick={() => removeFile(idx)} className="text-slate-400 hover:text-coral">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Form Actions */}
        <div className="pt-6 border-t border-slate-200 flex flex-col sm:flex-row justify-end items-center gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="w-full sm:w-auto px-5 py-3 border border-slate-300 rounded-button text-xs font-extrabold text-slate-700 hover:bg-slate-100 transition-colors"
          >
            Annuler
          </button>

          <button
            type="submit"
            disabled={isDuplicate}
            className={`w-full sm:w-auto px-7 py-3 rounded-button text-xs font-black uppercase tracking-wider transition-all shadow-lime-sm ${
              isDuplicate
                ? 'bg-slate-300 text-slate-500 cursor-not-allowed opacity-70'
                : 'bg-lime hover:bg-lime-hover text-midnight hover:shadow-lime-glow'
            }`}
          >
            Enregistrer et activer le bail (DH)
          </button>
        </div>

      </form>

    </div>
  );
}

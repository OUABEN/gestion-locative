import React, { useState } from 'react';
import { X, UserPlus, AlertTriangle, CheckCircle, UploadCloud, FileText, Trash2, CreditCard } from 'lucide-react';

export default function NouveauLocataireModal({ isOpen, onClose, onSave, existingCins }) {
  if (!isOpen) return null;

  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    cin: '',
    email: '',
    telephone: ''
  });

  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [errorCin, setErrorCin] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);

  // Normalisation automatique et détection en direct du doublon CIN
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
    const newFiles = files.map((file, idx) => ({
      id: `up-${Date.now()}-${idx}`,
      nom: file.name,
      taille: `${(file.size / (1024 * 1024)).toFixed(1)} Mo`,
      dateUpload: new Date().toLocaleDateString('fr-FR')
    }));
    setUploadedFiles([...uploadedFiles, ...newFiles]);
  };

  const removeFile = (id) => {
    setUploadedFiles(uploadedFiles.filter(f => f.id !== id));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setIsSubmitted(true);

    const normalizedCin = formData.cin.trim().toUpperCase();

    // Règle d'intégrité stricte UML (Diagramme d'activités & Séquence)
    if (existingCins.includes(normalizedCin)) {
      setErrorCin(`Fiche refusée : CIN déjà utilisé (${normalizedCin})`);
      return;
    }

    if (!formData.nom || !formData.prenom || !formData.cin || !formData.email || !formData.telephone) {
      return;
    }

    onSave({
      nom: formData.nom.trim(),
      prenom: formData.prenom.trim(),
      cin: normalizedCin,
      email: formData.email.trim(),
      telephone: formData.telephone.trim(),
      hasBailActif: false,
      bailInfo: null,
      piecesJustificatives: uploadedFiles
    });

    onClose();
  };

  const isCinDuplicate = Boolean(errorCin);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 modal-backdrop">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-xl w-full max-h-[92vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        
        {/* En-tête de la modale */}
        <div className="px-6 py-4 bg-navy-700 text-white flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-lg bg-navy-600 flex items-center justify-center text-sky-300">
              <UserPlus className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-base tracking-tight">Nouvelle fiche locataire (F-LOC-01)</h2>
              <p className="text-xs text-slate-300">Formulaire d'enregistrement avec contrôle d'unicité CIN</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-300 hover:text-white p-1 rounded-lg hover:bg-navy-600 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Corps du formulaire */}
        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto space-y-4 flex-1">
          
          {/* Alerte Erreur HTTP 409 si CIN déjà utilisé */}
          {errorCin && (
            <div className="p-3.5 bg-red-50 border border-red-200 rounded-xl flex items-start space-x-3 text-red-800 animate-in fade-in duration-200">
              <AlertTriangle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div className="text-xs">
                <span className="font-bold text-red-900 block text-sm">
                  {errorCin}
                </span>
                <p className="text-red-700 mt-0.5">
                  Code erreur : <strong>HTTP 409 Conflict</strong> (Règle d'intégrité UML p. 5 & 6). Chaque locataire doit posséder un CIN unique dans la base de données.
                </p>
              </div>
            </div>
          )}

          {/* Grille Nom & Prénom */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Nom de famille <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                placeholder="ex. Benjelloun"
                value={formData.nom}
                onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
                className="w-full px-3.5 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-navy-600"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Prénom <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                placeholder="ex. Mehdi"
                value={formData.prenom}
                onChange={(e) => setFormData({ ...formData, prenom: e.target.value })}
                className="w-full px-3.5 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-navy-600"
              />
            </div>
          </div>

          {/* Numéro CIN avec validation d'unicité */}
          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="block text-xs font-semibold text-slate-700 uppercase">
                Numéro de CIN <span className="text-red-500">*</span> [Clé Métier Unique]
              </label>
              {formData.cin.trim().length >= 4 && !errorCin && (
                <span className="text-[11px] font-medium text-emerald-600 flex items-center">
                  <CheckCircle className="w-3.5 h-3.5 mr-1" />
                  CIN unique et disponible
                </span>
              )}
            </div>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <CreditCard className="w-4 h-4" />
              </div>
              <input
                type="text"
                required
                placeholder="ex. EF345678 (Normalisé majuscule)"
                value={formData.cin}
                onChange={handleCinChange}
                className={`w-full pl-9 pr-4 py-2 font-mono text-sm border rounded-lg focus:outline-none transition-colors ${
                  isCinDuplicate
                    ? 'border-red-500 bg-red-50/40 text-red-900 focus:ring-2 focus:ring-red-400'
                    : 'border-slate-200 focus:ring-2 focus:ring-navy-600'
                }`}
              />
            </div>
            <p className="text-[11px] text-slate-400 mt-1">
              Exemple de CIN existant pour tester le rejet 409 : <code className="bg-slate-100 text-slate-700 px-1 py-0.5 rounded cursor-pointer font-bold" onClick={() => handleCinChange({ target: { value: 'AB123456' } })}>AB123456</code>
            </p>
          </div>

          {/* Email et Téléphone */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Adresse Email <span className="text-red-500">*</span>
              </label>
              <input
                type="email"
                required
                placeholder="nom@exemple.ma"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="w-full px-3.5 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-navy-600"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Téléphone <span className="text-red-500">*</span>
              </label>
              <input
                type="tel"
                required
                placeholder="+212 6 XX XX XX XX"
                value={formData.telephone}
                onChange={(e) => setFormData({ ...formData, telephone: e.target.value })}
                className="w-full px-3.5 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-navy-600"
              />
            </div>
          </div>

          {/* Zone d'upload Pièces Justificatives */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
              Pièces Justificatives (CIN recto/verso, contrat de travail, etc.)
            </label>
            <div className="border-2 border-dashed border-slate-200 hover:border-navy-400 rounded-xl p-4 text-center bg-slate-50/50 hover:bg-slate-50 transition-colors cursor-pointer relative">
              <input
                type="file"
                multiple
                onChange={handleFileUpload}
                className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              />
              <UploadCloud className="w-7 h-7 text-navy-600 mx-auto mb-1.5" />
              <p className="text-xs font-medium text-slate-700">
                Glissez vos documents ici ou <span className="text-navy-700 underline font-semibold">parcourez</span>
              </p>
              <p className="text-[10px] text-slate-400 mt-0.5">Formats acceptés : PDF, PNG, JPG (Max 10 Mo)</p>
            </div>

            {/* Liste des fichiers téléversés */}
            {uploadedFiles.length > 0 && (
              <div className="mt-2.5 space-y-1.5">
                {uploadedFiles.map((file) => (
                  <div
                    key={file.id}
                    className="flex items-center justify-between px-3 py-1.5 bg-slate-100/70 border border-slate-200 rounded-lg text-xs"
                  >
                    <div className="flex items-center space-x-2 truncate">
                      <FileText className="w-3.5 h-3.5 text-navy-600 flex-shrink-0" />
                      <span className="font-medium text-slate-700 truncate">{file.nom}</span>
                      <span className="text-[10px] text-slate-400">({file.taille})</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeFile(file.id)}
                      className="text-slate-400 hover:text-red-600 ml-2"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

        </form>

        {/* Pied de la modale */}
        <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-200 flex justify-end space-x-2.5">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 border border-slate-300 text-slate-700 hover:bg-slate-100 rounded-lg text-xs font-semibold transition-colors"
          >
            Annuler
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={isCinDuplicate}
            className={`px-5 py-2 text-white text-xs font-semibold rounded-lg shadow-sm transition-all ${
              isCinDuplicate
                ? 'bg-slate-300 cursor-not-allowed opacity-60'
                : 'bg-navy-700 hover:bg-navy-800'
            }`}
          >
            Enregistrer la fiche
          </button>
        </div>

      </div>
    </div>
  );
}

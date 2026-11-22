import runpy
from http.server import ThreadingHTTPServer

# Réutilise le simulateur existant sans démarrer son main local.
module = runpy.run_path("/demo/serveur_demo.py", run_name="darloc_stubs_docker")
with ThreadingHTTPServer(("0.0.0.0", 8091), module["Gestionnaire"]) as serveur:
    print("Services SIMULES Docker sur le port interne 8091", flush=True)
    serveur.serve_forever()

# Vérification Android 2.0.1-phone — 7 novembre 2026

Cette version ajoute une vraie inférence de texte sur le téléphone à la compagne native. Elle ne réalise pas l’ensemble de A.docx et ne fournit pas les privilèges système d’un assistant installé par le constructeur.

## Résultats obtenus

| Vérification | Résultat |
|---|---|
| Compilation application et APK de test | Réussie avec JDK 17, Gradle 8.11.1, AGP 8.10.1, Kotlin/Compose 2.2.21, SDK/Build Tools 35 |
| Tests JVM Android | 11 réussis, 0 échec : 7 tests historiques + 4 pour les modes gratuits, le modèle figé, le contexte borné et le filtrage des marqueurs |
| Signature APK | Vérifiée ; même certificat de débogage que l’APK Companion précédent, versionCode 2, versionName 2.0.1-phone |
| Compatibilité déclarée | Minimum API 26, cible API 35 ; moteur LiteRT-LM fourni en ARM64 et x86_64 |
| Intégrité du modèle récupéré sur PC | Taille et SHA-256 conformes à la révision figée du guide |
| Test instrumental Android | 1 réussi, 0 échec : vrai moteur CPU, vraie génération, import corrompu refusé et modèle existant préservé |
| Réseau pendant l’inférence | Émulateur en mode avion (`airplane_mode_on=1`), Wi-Fi et données désactivés |
| Conversation depuis le champ natif | Question saisie, Envoyer, réponse affichée et historique de session passé à 2 messages |
| Import par le sélecteur Android | Fichier choisi dans Téléchargements ; copie privée et message « Modèle prêt · vérification SHA-256 réussie » observés |
| Plantage observé | Aucun message AndroidRuntime fatal pendant ces essais |

L’essai utilise un émulateur Android API 35 x86_64 dédié, doté de 3072 Mio de RAM. Le premier test instrumental a mis **48,445 secondes pour charger le moteur et produire la réponse** ; ce chiffre est propre à cet émulateur sur le PC chargé, et ne prédit pas la vitesse de l’A53. Le modèle était provisionné dans le stockage privé pour ce test ; l’import via le sélecteur a ensuite été vérifié séparément avec le même fichier.

Pour « Presente-toi en une phrase en francais. », le champ natif a affiché : « Bonjour! Je suis ANGE, une compagne IA locale et respectueuse. 🌍 ». La capture [phone-response.png](verification/phone-response.png) montre l’interface et l’icône mode avion. Les hiérarchies XML et journaux synthétiques sont conservés dans `verification/phone-*`.

Le test de conseils d’organisation a aussi produit un texte français maladroit et peu utile. **La réussite du test prouve l’exécution locale, pas une qualité comparable à un grand modèle cloud.** Les réponses de ce modèle 0.6B restent à évaluer, particulièrement en français et en malgache. Le contexte est court et le modèle peut inventer des faits.

## Ce qui n’a pas été validé sur appareil physique

- A53 SM-A536B/DS réel : vitesse, mémoire disponible, stabilité prolongée, chauffe, consommation et Android 16.
- Son réellement audible : l’émulateur était lancé sans sortie audio. Les retours du moteur Android ont parcouru le cycle de lecture, mais une écoute et une voix française locale doivent être testées sur le Samsung.
- Reconnaissance vocale française en mode avion et disponibilité des packs linguistiques sur ce téléphone.
- Téléchargement de 347 Mo depuis le bouton de l’APK sur le réseau du téléphone : le fichier public exact a été téléchargé et haché sur PC, puis l’import Android réel a été validé. Le transfert réseau n’a pas été répété dans l’APK. Pas de reprise automatique d’un téléchargement interrompu.
- Essais prolongés ou robustesse sur une grande variété d’appareils, compatibilité 32 bits du moteur : seules ses bibliothèques ARM64 et x86_64 sont fournies.

Le code web/PC est conservé, avec ses limites et ses essais antérieurs documentés dans `ETAT_COMPANION.md`. Le nouveau contrôleur Android n’appelle aucun fournisseur cloud, refuse un mode cloud réinjecté, et envoie `cloud_consent=false` pour l’option PC local. Aucun appel IA payant n’a été effectué dans ces essais. Les fichiers du serveur personnel, son jeton, ses certificats et le contenu de `sources/` n’ont pas été modifiés par cette évolution.

L’application reste un APK de débogage pour essais personnels. Le modèle n’a aucun outil d’exécution : pas d’appels/SMS, vision, rappels système, contrôle d’applications ou réveil vocal permanent. L’animation reste une animation 2D à huit poses avec synchronisation vocale approximative.

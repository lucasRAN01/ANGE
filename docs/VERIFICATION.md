# Vérification de la révision graphique fidèle

Contrôles réalisés le 6 novembre 2026 sur les fichiers de cette révision. Les résultats ne concernent pas un assistant IA complet.

## Comparaison visuelle

La capture `verification/reference-reproduction.png` est produite par le navigateur à partir des composants de l’interface, et non à partir de l’affiche originale. La planche mesure 1536 × 1024 comme votre image. Le contrôle automatique vérifie la géométrie de la fenêtre PC : x 196, y 144, largeur 794 px. Le téléphone, la compagne et les six panneaux sont des éléments distincts ; leur géométrie est enregistrée dans `verification/showcase-results.json`.

Les captures ont été examinées pour ajuster la position de la recherche, du texte d’accueil, des cartes, des panneaux inférieurs et des éléments du téléphone. `comparaison.html` fournit un comparateur visuel avec curseur. La fidélité est évaluée visuellement ; aucun score de ressemblance pixel à pixel n’est inventé. Les illustrations sont recréées et certains textes ont été adaptés pour distinguer les statuts UI des services non implémentés.

## Tests Windows

Les scripts Playwright ouvrent réellement le fichier HTML local dans Chrome sans interface visible. `tests/ui-test.cjs` couvre les dix écrans dans la vue application : navigation, sélection du menu, captures, saisie du chat et échappement HTML, notes CRUD, export/import JSON, refus d’un import incompatible, persistance après rechargement, préférence de langue, six états vocaux, parcours de liaison fictif, annulation d’un contrôle et arrêt des animations. Chaque écran est contrôlé à 390 px pour détecter un débordement horizontal de la page. Le menu compact est volontairement défilant.

`tests/showcase-test.cjs` couvre la planche complète à 1536 px : géométrie, présence de six panneaux HTML, six fonctionnalités et six raccourcis mobiles, chargement des images, navigation vocale du téléphone, bouton d’écoute simulée, interrupteur, choix de langue, saisie depuis le petit panneau Chat, accès aux modules supplémentaires, réduction et restauration de la fenêtre.

Les deux scripts enregistrent leurs résultats dans `verification/ui-results.json` et `verification/showcase-results.json`. Ils vérifient l’absence d’erreurs JavaScript pendant les parcours. Le test de planche détecte aussi les requêtes de fichiers échouées et les images non chargées. Le test principal vérifie l’absence de requêtes HTTP/HTTPS. Les trois fichiers JavaScript applicatifs ont été contrôlés par `node --check`.

Pour les relancer, installer Node, Playwright et Chrome, puis exécuter `node tests/ui-test.cjs` et `node tests/showcase-test.cjs`. La variable `PLAYWRIGHT_MODULE` permet d’indiquer un module Playwright déjà installé. L’ouverture de l’interface ne nécessite pas ces outils de test.

## Android

La syntaxe de `MainActivity.kt` et des trois fichiers `.gradle.kts` a été analysée sans erreur par le parseur Kotlin livré avec Android Studio, après mise à jour du design et des ressources. Le programme est fourni dans `tests/CheckKotlinSyntax.java`, et la trace dans `verification/kotlin-syntax.txt`. Ce contrôle ne résout pas les dépendances et ne vérifie pas les types Compose.

Les ressources d’illustration sont embarquées dans `drawable-nodpi` et référencées par le code Compose. Les XML du manifeste et de l’icône sont bien formés. L’accueil et les tuiles sont des composants natifs ; le téléphone HTML de la planche n’est pas présenté comme une capture Android exécutée.

**Compilation APK, installation et navigation Android non validées.** Le JDK 17 et le SDK 35 sont présents, mais le projet source ne possède pas de Wrapper Gradle et aucune distribution Gradle complète n’a été obtenue lors des tentatives précédentes. Les téléchargements incomplets n’ont pas été utilisés ni inclus. Aucun APK ou résultat de compilation n’est revendiqué pour cette révision. Les essais Android 16, rotation, restauration d’état, grande police et TalkBack restent à faire sur appareil ou émulateur.

## Intégrité et périmètre

Le manifeste SHA-256 inventorie les fichiers, hors le manifeste lui-même. Le ZIP final est réouvert, chaque entrée est lue et comparée au fichier livré, et une somme SHA-256 accompagne l’archive. Les fichiers synchronisés de `sources` sont restés en lecture seule.

Les PNG générés, leurs versions WebP et les prompts sont fournis. Les éléments de l’illustration ne sont pas des preuves d’un service IA actif. La reconnaissance vocale, les moteurs IA, les connexions, le contrôle des appareils, la vision et la sécurité du futur backend restent non implémentés. Les graphiques de la planche sont purement illustratifs.

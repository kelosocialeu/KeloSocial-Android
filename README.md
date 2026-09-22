# Kelo Social Android

Application Android officielle de Kelo Social.

- Site : https://kelosocial.eu
- PDS : https://pds.kelosocial.eu
- Protocole : AT Protocol
- Langage : Kotlin
- Moteur : Android WebView + composants Android natifs

## Fonctionnalités préparées

- Icône Kelo Social avec la palette de la marque.
- Ouverture de Kelo Social dans une application Android dédiée.
- Téléphones et tablettes.
- Sélection d'images et de vidéos depuis l'appareil.
- Autorisation appareil photo prête pour les fonctions média natives.
- Autorisation de notifications Android 13+ et canal Kelo Social.
- Navigation limitée au domaine Kelo Social.

## Développement

Ouvrir le dépôt dans Android Studio, synchroniser Gradle puis lancer la configuration app.

Le dépôt utilise volontairement une activité Android native avec WebView afin de réutiliser l'expérience Kelo Social existante.

## Permissions

Android 13+ utilise POST_NOTIFICATIONS pour les notifications. Pour les images et vidéos, Android recommande le Photo Picker lorsque l'application n'a pas besoin d'un accès permanent à toute la galerie.

L'accès à la caméra doit être demandé avant son utilisation.

## Sécurité

Aucun secret, token privé ou identifiant d'administration ne doit être commité dans ce dépôt.

# SubTrack

**Plateforme de gestion et d'optimisation des abonnements SaaS.**

Netflix, Spotify, Slack, Adobe, iCloud, ChatGPT… Les abonnements s'accumulent, se renouvellent
en silence et sont facturés dans des devises différentes. SubTrack les réunit en un seul endroit :
vous savez combien vous dépensez réellement, vous êtes prévenu avant chaque prélèvement et vous
repérez les abonnements inutiles.

**Stack :** Java 17 · Jakarta EE 10 (Faces, CDI, JPA, EJB Timers) · Hibernate 6.4 · PostgreSQL ·
Flyway · PrimeFaces · WildFly 31 · Google Gemini

---

## Le problème que SubTrack résout

| Problème | Solution apportée par SubTrack |
|---|---|
| On ne sait plus combien on paie au total chaque mois. | Un tableau de bord qui additionne tous les abonnements, au mois et à l'année. |
| Les abonnements sont facturés en USD, EUR, MAD… et ne s'additionnent pas. | Conversion automatique dans une devise d'affichage unique, avec des taux de change mis à jour toutes les 6 heures. |
| On découvre un renouvellement une fois le prélèvement effectué. | Des alertes configurables (e-mail, Telegram, WhatsApp) X jours avant chaque échéance. |
| On paie deux fois le même service, ou des services qu'on n'utilise plus. | Détection des doublons et des abonnements inutilisés, avec l'économie mensuelle estimée. |
| Saisir chaque abonnement à la main est fastidieux. | Import des factures depuis Gmail et extraction automatique (service, montant, devise, date) par l'IA Gemini. |
| Les données sont éparpillées et impossibles à analyser. | Graphiques par catégorie, tendance sur 6 mois, classement des coûts, et export CSV/JSON. |

---

## Fonctionnalités

### Pour les utilisateurs

- **Tableau de bord** : dépenses mensuelles et annuelles, prochain renouvellement, échéances
  des 30 prochains jours, répartition par catégorie (graphique en anneau) et abonnements les plus
  coûteux.
- **Gestion des abonnements** : ajout, modification, pause, annulation et réactivation. La date
  du prochain prélèvement avance automatiquement à chaque cycle (hebdomadaire, mensuel,
  trimestriel, annuel).
- **Multi-devises** : chaque abonnement garde sa devise d'origine ; tous les totaux sont convertis
  dans la devise d'affichage (MAD par défaut).
- **Alertes de renouvellement** : par e-mail, Telegram ou WhatsApp, envoyées une seule fois par
  cycle, le nombre de jours choisi avant l'échéance.
- **Analyses** : tendance des dépenses sur 6 mois, répartition par statut et par cycle de
  facturation, dépenses par catégorie, classement complet des abonnements.
- **Suggestions d'économies** : doublons et abonnements sans historique d'utilisation.
- **Import de factures Gmail** : connexion OAuth 2.0 à Gmail, lecture des factures et création des
  abonnements correspondants grâce à Gemini.
- **Export** : abonnements et historique de paiements en CSV, sauvegarde complète du compte en JSON.
- **Compte** : profil, préférences de notification et réinitialisation du mot de passe par e-mail.

### Pour les administrateurs

- **Tableau de bord administrateur** : utilisateurs (actifs/suspendus), abonnements suivis,
  dépenses totales de la plateforme, inscriptions des 6 derniers mois, services les plus suivis,
  activité récente et état des intégrations.
- **Gestion des utilisateurs** : consultation détaillée, suspension et réactivation des comptes
  (effet immédiat, même sur une session ouverte).
- **Catalogue SaaS** : services proposés aux utilisateurs avec prix et devise par défaut.
- **Taux de change** : consultation et rafraîchissement manuel des taux.
- **Configuration** : clés API (Gemini, taux de change, SMTP, Google OAuth, Telegram, WhatsApp)
  stockées en base, avec un bouton de test de connexion pour chacune.

### Tâches automatiques

| Tâche | Fréquence | Rôle |
|---|---|---|
| `BillingScheduler` | tous les jours à 00:05 | Fait avancer les dates de prélèvement passées au cycle suivant. |
| `AlertScheduler` | tous les jours à 08:00 | Envoie les alertes de renouvellement du jour. |
| `ExchangeRateScheduler` | toutes les 6 heures | Met à jour les taux de change. |
| `InvoiceScheduler` | toutes les 5 minutes | Analyse les factures importées (5 tentatives maximum par facture). |

---

## Sécurité

- Mots de passe hachés avec **BCrypt** ; jetons de réinitialisation à usage unique, valables 1 heure,
  dont seul le hash SHA-256 est stocké.
- **Filtre d'authentification** central : toutes les pages exigent une connexion, et `/admin/*`
  exige le rôle administrateur. L'utilisateur est relu en base à chaque requête.
- **Protection contre le brute force** : 5 échecs par e-mail ou 20 par adresse IP en 15 minutes ;
  3 e-mails de réinitialisation par heure et par adresse.
- Pas de divulgation des comptes existants (même réponse à la connexion et au mot de passe oublié).
- Contrôle de propriété : un utilisateur ne peut ni voir ni modifier les abonnements d'un autre.
- Paramètre `state` vérifié lors de la connexion Gmail (protection CSRF), ID de session renouvelé
  à la connexion, cookies `HttpOnly` et `Secure`.
- Les secrets de configuration ne sont jamais renvoyés au navigateur, et aucun identifiant
  n'est écrit dans le code : tout passe par des variables d'environnement.
- Les messages d'erreur techniques sont journalisés côté serveur et jamais affichés aux utilisateurs.

---

## Architecture

Application Jakarta EE en couches (N-tiers) :

```
Pages JSF (.xhtml)  →  Beans (controller)  →  Services  →  DAO  →  Entités JPA  →  PostgreSQL
                                                  ↓
                     APIs externes : Gemini, Gmail, taux de change, SMTP, Telegram, WhatsApp
```

```
SubTrack/
├── pom.xml
├── .env.example                  # toutes les variables d'environnement utilisées
├── wildfly/                      # scripts CLI WildFly exécutés par le plugin Maven
├── docs/                         # rapport du projet, modèle de données, charte graphique, choix techniques
└── src/
    ├── main/
    │   ├── java/com/subtrack/
    │   │   ├── controller/       # beans JSF des pages utilisateur (+ UserContext, servlet OAuth)
    │   │   │   └── admin/        # beans JSF des pages /admin/*
    │   │   ├── service/          # logique métier, tâches planifiées, APIs externes
    │   │   ├── dao/              # accès aux données (interface + implémentation JPA)
    │   │   ├── entity/           # entités JPA
    │   │   ├── enums/
    │   │   ├── converter/        # convertisseurs JSF
    │   │   ├── filter/           # filtres d'authentification et d'encodage
    │   │   ├── exception/        # gestion globale des erreurs JSF
    │   │   └── util/             # initialisation au démarrage, hachage, journalisation
    │   ├── resources/
    │   │   ├── db/migration/     # migrations SQL Flyway (V1, V2, V3…)
    │   │   ├── META-INF/persistence.xml
    │   │   └── messages.properties
    │   └── webapp/
    │       ├── *.xhtml           # pages utilisateur (tableau de bord, analyses, factures…)
    │       ├── subscriptions/    # liste, création, modification, détail
    │       ├── admin/            # pages administrateur
    │       ├── templates/        # gabarits et menus latéraux
    │       ├── resources/        # CSS et images
    │       └── WEB-INF/          # web.xml, faces-config.xml, beans.xml
    └── test/                     # tests unitaires (JUnit 5 + Mockito)
```

---

## Installation et lancement

### Prérequis

- JDK 17
- PostgreSQL 14 ou plus récent
- Maven n'est pas nécessaire : le wrapper `mvnw` est fourni. WildFly est téléchargé et
  configuré automatiquement par le plugin Maven.

### Étapes

1. **Créer la base de données :**

   ```sql
   CREATE DATABASE subtrack;
   ```

2. **Définir les variables d'environnement** (la liste complète est dans [.env.example](.env.example)) :

   | Variable | Obligatoire | Rôle |
   |---|---|---|
   | `SUBTRACK_DB_PASSWORD` | oui | Mot de passe PostgreSQL |
   | `SUBTRACK_DB_URL`, `SUBTRACK_DB_USER` | non | Par défaut `jdbc:postgresql://localhost:5432/subtrack` et `postgres` |
   | `SUBTRACK_ADMIN_EMAIL`, `SUBTRACK_ADMIN_PASSWORD` | au premier lancement | Crée le premier compte administrateur |
   | `SUBTRACK_PROJECT_STAGE=Development` | en local | Messages JSF détaillés |
   | `SUBTRACK_SECURE_COOKIES=false` | en local hors HTTPS | Autorise le cookie de session en HTTP |
   | `APP_BASE_URL` | en production | URL publique utilisée dans les e-mails de réinitialisation |

   Exemple sous PowerShell :

   ```powershell
   $env:SUBTRACK_DB_PASSWORD = "votre_mot_de_passe"
   $env:SUBTRACK_ADMIN_EMAIL = "admin@exemple.com"
   $env:SUBTRACK_ADMIN_PASSWORD = "MotDePasse123"
   $env:SUBTRACK_PROJECT_STAGE = "Development"
   ```

3. **Lancer l'application :**

   ```
   ./mvnw wildfly:dev
   ```

   L'application est disponible sur `http://localhost:8080/subtrack/`.

4. **Configurer les intégrations** : connectez-vous avec le compte administrateur, puis ouvrez
   **Admin → Configuration** pour renseigner les clés API (Gemini, taux de change, SMTP,
   Google OAuth, Telegram, WhatsApp).

### Base de données

Le schéma est créé et mis à jour automatiquement par **Flyway** au démarrage. Pour le modifier,
ajoutez un nouveau fichier `src/main/resources/db/migration/V<n>__description.sql`. Ne modifiez
jamais une migration déjà exécutée.

### Journaux

Les journaux de l'application sont écrits dans `standalone/log/server.log` de WildFly.

---

## Tests

```
./mvnw test
```

Les tests unitaires (JUnit 5 et Mockito, sans base de données) couvrent les services
(abonnements, clients, alertes, factures, devises), les calculs du tableau de bord, le passage au
cycle de facturation suivant, la réinitialisation du mot de passe et la limitation des tentatives
de connexion.

---

## Documentation

- [Rapport du projet](docs/rapport-subtrack.pdf)
- [Modèle de données](docs/data-model.md)
- [Charte graphique](docs/design-rules.md)
- [Choix techniques](docs/technical-decisions.md)

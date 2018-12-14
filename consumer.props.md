# Build Properties
Generally, properties can be set in the following places, in order of precedence (higher overrides lower):

- Gradle means
Consult [Gradle documentation](https://docs.gradle.org/current/userguide/build_environment.html) for the build's target version.
- command-line
  - a project's gradle.properties
  - a user's gradle.properties at GRADLE_USER_HOME
- build means
Changing a property in the build scripts will be committed to the repo and generally override all consumers' properties.

If `putIfAbsent` is used, the assignment will take lowest precedence in the hierarchy above.

By default, AcornUi projects favor `putIfAbsent` unless it is necessary to break from the convention.

To ensure a property is unset/clear regardless of whether a property has been set via Gradle means, set the property to an empty string without using `putIfAbsent`

## Changeable Properties
Some of the changeable properties have already been listed under `val props` maps in the root and app directories' `settings.gradle.kts` while the definitive list is being built below.

<!-- Todo: in progress -->
<!--	// The app skin must be specified in the app directory's settings.gradle.kts, gradle.properties, or at the
	// command-line.

	// Available skins include those provided by acornUi and those found in the APP_SKINS_PATH. - APP_SKIN

	// Put custom ui skins for acornUi here. - app_skins_path-->
# Git Data Library (GDL)

Git Data Library (GDL) is a Java library that provides a simple and efficient way to store, retrieve, and manage data 
objects using Git as a persistent storage backend. This library allows you to treat Git repositories as data stores, 
with automatic versioning, history tracking, and collaboration capabilities inherent to Git.

## Features

- Store Plain Old Java Objects (data-objects) in a Git repository
- Encrypt certain data-object properties or the whole data-object with custom secrets
- Sync data to a remote repository using SSH (with multiple supported authentication methods (see package **io.github.zeront4e.gdl.configurations.auth**))
- Store certain field-properties cached in memory

## Installation

The library is available from the central Maven repository. You can import the library using the following snippet.

### Maven

```xml
<dependency>
    <groupId>io.github.zeront4e</groupId>
    <artifactId>git-data-lib</artifactId>
    <version>1.2.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.zeront4e.gdl:git-data-lib:1.2.1'
```

## Getting Started

### Local repository configuration

First it is necessary to create a configuration for a Git repository. There are multiple ways to create a configuration.

The most basic configuration option is just to provide a local repository directory:

```java
import io.github.zeront4e.gdl.configurations.common.GdlLocalConfiguration;
import io.github.zeront4e.gdl.GdlGitConfiguration;

import java.io.File;
import java.util.Map;

public static void main(String[] args) {
    File repoDir = new File("/path/to/local/repo");

    //Provide optional secrets to encrypt your data-objects with, if they should be protected (see examples below).
    Map<String, String> credentials = Map.of(
            "secretName1", "secretValue1",
            "secretName2", "secretValue2"
    );
    
    String branch = "main";

    GdlLocalConfiguration configuration = new GdlLocalConfiguration(repoDir, credentials, branch);

    GdlGitConfiguration gitConfig = new GdlGitConfiguration(configuration);
}
```

### Remote repository configuration

If you want to push the data to a remote repository you must provide a more detailed configuration. You can specify a 
custom branch, the remote URL to the existing repository to clone and sync data to, and an optional push delay, 
before changes are actually pushed to the remote repository.

There are also multiple ways to authenticate against the remote repository (origin):

  - Credentials based authentication (username and password)
  - Token based authentication (a platform issued token)
  - Asymmetric key based authentication (using SSH keys)

**Credentials based authentication:**

```java

import io.github.zeront4e.gdl.GdlOfflineConfiguration;
import io.github.zeront4e.gdl.GdlGitConfiguration;
import io.github.zeront4e.gdl.configurations.auth.GdlPasswordBasedHttpConfiguration;
import io.github.zeront4e.gdl.configurations.common.GdlOnlineConfiguration;
import io.github.zeront4e.gdl.configurations.auth.GdlPasswordBasedSshConfiguration;

import java.io.File;
import java.util.Map;

public static void main(String[] args) {
    File repoDir = new File("/path/to/local/repo");

    Map<String, String> credentials = Map.of(
            "secretName1", "secretValue1",
            "secretName2", "secretValue2"
    );

    String branch = "main";

    GdlOnlineConfiguration configuration = new GdlOnlineConfiguration(
            repoDir,
            credentials,
            branch,
            "https://github.com/user/repo",   //remote repository URL
            0                                 //push delay in milliseconds (or 0 to set none)
    );

    File knownHostsFile = new File(".ssh/known_hosts");

    //Credentials based authentication:

    GdlPasswordBasedHttpConfiguration passwordBasedHttpConfiguration = new GdlPasswordBasedHttpConfiguration("username",
            "password", knownHostsFile);

    GdlGitConfiguration gdlGitConfiguration = new GdlGitConfiguration(configuration,
            passwordBasedHttpConfiguration);
}
```

**Token based authentication:**

Please use the class 'GdlGitHubTokenBasedHttpConfiguration' to authenticate against GitHub using a classic personal access token.

```java
import io.github.zeront4e.gdl.configurations.auth.GdlTokenBasedHttpConfiguration;
import io.github.zeront4e.gdl.configurations.common.GdlLocalConfiguration;
import io.github.zeront4e.gdl.GdlOfflineConfiguration;
import io.github.zeront4e.gdl.GdlGitConfiguration;

import java.io.File;
import java.util.Map;

public static void main(String[] args) {
    File repoDir = new File("/path/to/local/repo");

    Map<String, String> credentials = Map.of(
            "secretName1", "secretValue1",
            "secretName2", "secretValue2"
    );

    String branch = "main";

    GdlLocalConfiguration configuration = new GdlLocalConfiguration(
            repoDir,
            credentials,
            branch,
            "https://github.com/user/repo",   //remote repository URL
            0                                 //push delay in milliseconds (or 0 to set none)
    );

    File knownHostsFile = new File(".ssh/known_hosts");

    //Token based authentication:

    GdlTokenBasedHttpConfiguration tokenBasedHttpConfiguration = new GdlTokenBasedHttpConfiguration("token",
            knownHostsFile);

    GdlGitConfiguration gdlGitConfiguration = new GdlGitConfiguration(configuration,
            tokenBasedHttpConfiguration);
}
```

**Asymmetric key based authentication:**

```java
import io.github.zeront4e.gdl.configurations.common.GdlLocalConfiguration;
import io.github.zeront4e.gdl.GdlOfflineConfiguration;
import io.github.zeront4e.gdl.GdlGitConfiguration;
import io.github.zeront4e.gdl.configurations.auth.GdlKeyBasedSshConfiguration;

import java.io.File;
import java.util.Map;

public static void main(String[] args) {
    File repoDir = new File("/path/to/local/repo");

    Map<String, String> credentials = Map.of(
            "secretName1", "secretValue1",
            "secretName2", "secretValue2"
    );

    String branch = "main";

    GdlLocalConfiguration configuration = new GdlLocalConfiguration(
            repoDir,
            credentials,
            branch,
            "git@github.com:user/repo.git",   //remote repository URL
            0                                 //push delay in milliseconds (or 0 to set none)
    );

    File knownHostsFile = new File(".ssh/known_hosts");

    //Asymmetric key based authentication:

    File privateKeyFile = new File(".ssh/id_rsa");

    GdlKeyBasedSshConfiguration keyBasedSshConfiguration = new GdlKeyBasedSshConfiguration(privateKeyFile,
            knownHostsFile, "optionalKeyDecryptionPassword");

    GdlGitConfiguration gdlGitConfiguration = new GdlGitConfiguration(configuration,
            keyBasedSshConfiguration);
}
```

### Create a Gdl instance to manage data

After you created a configuration, you can create a Gdl instance, to add, query, update or delete data-objects.

```java
import io.github.zeront4e.gdl.*;
import io.github.zeront4e.gdl.annotations.GdlCachedProperty;
import io.github.zeront4e.gdl.configurations.common.GdlLocalConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//This is an example class to serialize.
//Note that you always MUST provide a default-constructor, if a non-default-constructor is present!

public static class AiPromptConfiguration {
    @GdlCachedProperty //We cache this property in memory.
    private String title;

    private String description;

    private String prompt;

    public AiPromptConfiguration() {
        //Ignore...
    }

    public AiPromptConfiguration(String title, String description, String prompt) {
        this.title = title;
        this.description = description;
        this.prompt = prompt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}

public static void main(String[] args) {
    //Simple configuration using a local repository:

    File repoDir = new File("/path/to/local/repo");

    String branch = "main";
    
    GdlLocalConfiguration configuration = new GdlLocalConfiguration(repoDir, Map.of(), branch);

    GdlGitConfiguration gdlGitConfiguration = new GdlGitConfiguration(configuration);

    //Create test-data.

    AiPromptConfiguration aiPromptConfiguration1 = new AiPromptConfiguration("title1", "description1", "prompt1");
    AiPromptConfiguration aiPromptConfiguration2 = new AiPromptConfiguration("title2", "description2", "prompt2");

    //Create a Gdl instance:

    Gdl gdl = new Gdl(gdlGitConfiguration);

    //Create a repository to perform CRUD operations:

    GdlDataRepository<AiPromptConfiguration> gdlDataRepository = gdl.getDataRepository(AiPromptConfiguration.class);

    //Add data to the repository:

    GdlData<AiPromptConfiguration> aiPromptConfigurationGdlData1 = gdlDataRepository.addData(aiPromptConfiguration1);
    GdlData<AiPromptConfiguration> aiPromptConfigurationGdlData2 = gdlDataRepository.addData(aiPromptConfiguration2);

    //Update data:

    String newTitle = aiPromptConfigurationGdlData1.getData().getTitle() + "-updated";

    aiPromptConfigurationGdlData1.getData().setTitle(newTitle);

    gdlDataRepository.updateData(aiPromptConfigurationGdlData1);

    //Query data (read from the disk):

    Optional<AiPromptConfiguration> optionalAiPromptConfiguration = gdlDataRepository.queryData()
            .filter(tmpData -> tmpData.getData().getTitle().endsWith("-updated"))
            .findFirst();

    System.out.println("The updated data title: " + (optionalAiPromptConfiguration.isPresent() ?
            optionalAiPromptConfiguration.get().getTitle() : "-"));

    //Query cached data (with properties stored in memory):

    List<AiPromptConfiguration> aiPromptConfigurations = gdlDataRepository.queryCachedData()
            .setLoadFilteredDataFromDisk(true) //We load found data-objects from disk (the data not stored in memory).
            .setFilter(tmpData -> tmpData.getData().getTitle().endsWith("-updated"));

    System.out.println("Found data-objects: " + aiPromptConfigurations.size());

    boolean wasDataDeleted = gdlDataRepository.deleteData(aiPromptConfigurationGdlData2);

    System.out.println("Was data deleted? -> " + wasDataDeleted);
}
```

### Encrypt data

This library provides two levels of encryption for your data:

1. **Field-level encryption** - Encrypt specific sensitive fields using `@GdlSecretProperty`
2. **Object-level encryption** - Encrypt the entire data-object using `@GdlSecretDataRepository`

You can use these encryption methods separately or combine them.

##### Field-level encryption

Use the `@GdlSecretProperty` annotation to encrypt specific fields within your data object:

```java
import io.github.zeront4e.gdl.annotations.GdlSecretProperty;

import java.util.Map;

public class UserCredentials {
    private String username; //Not encrypted.

    @GdlSecretProperty("password-secret")
    private String password; //Will be encrypted using the "password-secret" key.

    @GdlSecretProperty("api-secret")
    private String apiKey; //Will be encrypted using the "api-secret" key.

    public UserCredentials() {
        //Ignore...
    }

    public UserCredentials(String username, String password, String apiKey) {
        this.username = username;
        this.password = password;
        this.apiKey = apiKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
```

When you add this object to the repository, the annotated fields will be automatically encrypted before storage and 
decrypted when queried:

```java
import io.github.zeront4e.gdl.*;
import io.github.zeront4e.gdl.configurations.common.GdlLocalConfiguration;

import java.util.Map;
import java.util.Optional;

public static void main(String[] args) {
    //Create a configuration with secrets.

    Map<String, String> secrets = Map.of(
            "password-secret", "strong-password-encryption-key",
            "api-secret", "another-strong-encryption-key"
    );

    String branch = "main";

    GdlLocalConfiguration configuration = new GdlLocalConfiguration(repoDir, secrets, branch);
    
    GdlGitConfiguration gitConfig = new GdlGitConfiguration(configuration);
    
    Gdl gdl = new Gdl(gitConfig);

    //Create and store data.

    UserCredentials credentials = new UserCredentials();
    credentials.setUsername("john.doe");
    credentials.setPassword("secret123"); //Will be encrypted.
    credentials.setApiKey("api-xyz-123"); //Will be encrypted.

    GdlDataRepository<UserCredentials> repository = gdl.getDataRepository(UserCredentials.class);
    GdlData<UserCredentials> storedData = repository.addData(credentials);

    //The password and apiKey fields are automatically encrypted in the repository file
    //but are available in the decrypted form when accessed through the API.

    Optional<UserCredentials> optionalUserCredentials = repository.queryData()
            .filter(tmpData -> tmpData.getData().getApiKey().startsWith("api-xyz-"))
            .findFirst();

    System.out.println("Found API key: " + (optionalUserCredentials.isPresent() ?
            optionalUserCredentials.get().getApiKey() : "-"));
}
```

##### Object-level encryption

When using object-level encryption, the entire object is encrypted as a single unit. 

You can use the `@GdlSecretDataRepository` annotation to encrypt an entire data object:

```java
import io.github.zeront4e.gdl.annotations.GdlSecretProperty;
import io.github.zeront4e.gdl.annotations.GdlSecretDataRepository;

import java.util.Map;

@GdlSecretDataRepository("user-data-secret")
public class UserCredentials {
    private String username; //Will be encrypted using the "user-data-secret" key.

    @GdlSecretProperty("password-secret")
    private String password; //Will be encrypted using the "password-secret" key (and the "user-data-secret" key).

    @GdlSecretProperty("api-secret")
    private String apiKey; //Will be encrypted using the "api-secret" key (and the "user-data-secret" key).

    public UserCredentials() {
        //Ignore...
    }

    public UserCredentials(String username, String password, String apiKey) {
        this.username = username;
        this.password = password;
        this.apiKey = apiKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
```

You can add, query, update and delete data-objects as seen in the previous example. The only difference is to also 
provide the "user-data-secret"-secret.

### Use a custom repository name

By default, Gdl uses the class name of your data object as the repository name. However, you can specify a custom 
repository name using the `@GdlDataRepositoryName` annotation. This ensures that the repository name stays the same,
even if the data object class is renamed.

```java
import io.github.zeront4e.gdl.annotations.GdlDataRepositoryName;

@GdlDataRepositoryName("user-credentials")
public class UserCredentials {
    private String username;
    private String password;
    
    // Constructors, getters, setters...
}
```
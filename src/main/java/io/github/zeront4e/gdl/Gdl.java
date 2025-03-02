package io.github.zeront4e.gdl;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Gdl {
    private final Map<Class<?>, GdlDataRepository<?>> classGdlDataRepositoryMap =
            Collections.synchronizedMap(new HashMap<>());

    private final DataManager dataManager;

    /**
     * Constructs a new Gdl instance with the given GdlGitConfiguration.
     * @param gdlGitConfiguration The GdlGitConfiguration to use for this Gdl instance.
     */
    public Gdl(GdlGitConfiguration gdlGitConfiguration) {
        Map<String, String> secretNameSecretMap = gdlGitConfiguration.getBaseConfiguration().getSecretNameSecretMap();

        File repoDirectoryFile = gdlGitConfiguration.getBaseConfiguration().getLocalRepositoryDirectory();

        GitDataChangeInteractionManager gitDataChangeInteractionManager =
                new GitDataChangeInteractionManager(gdlGitConfiguration);

        this.dataManager = new DataManager(secretNameSecretMap, repoDirectoryFile, gitDataChangeInteractionManager);
    }

    /**
     * Returns the data repository for the given data type.
     * @param dataType The data type to create the repository for.
     * @return The newly created or cached data repository.
     * @param <Type> The type of the data object to store.
     */
    public <Type> GdlDataRepository<Type> getDataRepository(Class<Type> dataType) {
        if(classGdlDataRepositoryMap.containsKey(dataType))
            return (GdlDataRepository<Type>) classGdlDataRepositoryMap.get(dataType);

        GdlDataRepository<Type> gdlDataRepository = new GdlDataRepository<>(dataManager, dataType);

        classGdlDataRepositoryMap.put(dataType, gdlDataRepository);

        return gdlDataRepository;
    }
}

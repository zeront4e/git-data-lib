/*
Copyright 2025 zeront4e (https://github.com/zeront4e)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.github.zeront4e.gdl;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The main entry point for the Git data library (GDL).
 * This class provides access to data repositories that store and manage data objects in a Git repository.
 * It handles the initialization of the Git repository and manages data repositories for different data types.
 */
public class Gdl {
    private final Map<Class<?>, GdlDataRepository<?>> classGdlDataRepositoryMap =
            Collections.synchronizedMap(new HashMap<>());

    private final DataManager dataManager;

    /**
     * Constructs a new Gdl instance with the given GdlGitConfiguration.
     * @param gdlGitConfiguration The GdlGitConfiguration to use for this Gdl instance.
     * @throws GitAPIException Exception that occurs if the initial clone attempt of a remote repository fails.
     * @throws IOException Exception that occurs if the remote repository couldn't be cloned to the local filesystem or
     * an existing local repository couldn't be opened.
     */
    public Gdl(GdlGitConfiguration gdlGitConfiguration) throws GitAPIException, IOException {
        Map<String, String> secretNameSecretMap = gdlGitConfiguration.getGdlBaseConfiguration()
                .getSecretNameSecretMap();

        File repoDirectoryFile = gdlGitConfiguration.getGdlBaseConfiguration().getLocalRepositoryDirectory();

        //We always try to initialize the local repository first, if it doesn't exist.
        gdlGitConfiguration.setupLocalRepositoryOrFail();

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

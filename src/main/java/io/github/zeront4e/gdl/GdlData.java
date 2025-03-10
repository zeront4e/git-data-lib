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

import java.util.Objects;

/**
 * A container to store a data object along with additional metadata.
 * @param <Data> The type of the data object stored in this container.
 */
public class GdlData<Data> {
    private String id;

    private long createTimestamp;

    private long updateTimestamp;

    private Data data;

    GdlData() {
        //Ignore (necessary for deserialization)...
    }

    GdlData(String id, long createTimestamp, long updateTimestamp, Data data) {
        this.id = id;
        this.createTimestamp = createTimestamp;
        this.updateTimestamp = updateTimestamp;
        this.data = data;
    }

    /**
     * Returns the unique identifier of this data object.
     * @return The unique identifier of this data object.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the timestamp when this data object was created.
     * @return The creation timestamp.
     */
    public long getCreateTimestamp() {
        return createTimestamp;
    }

    /**
     * Returns the timestamp when this data object was last updated.
     * @return The update timestamp.
     */
    public long getUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * Returns the data object stored in this container.
     * @return The data object.
     */
    public Data getData() {
        return data;
    }

    void setId(String id) {
        this.id = id;
    }

    void setCreateTimestamp(long createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    void setUpdateTimestamp(long updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    void setData(Data data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GdlData<?> that = (GdlData<?>) o;
        return createTimestamp == that.createTimestamp && updateTimestamp == that.updateTimestamp &&
                Objects.equals(id, that.id) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, createTimestamp, updateTimestamp, data);
    }

    @Override
    public String toString() {
        return "GdlData{" +
                "id='" + id + '\'' +
                ", createTimestamp=" + createTimestamp +
                ", updateTimestamp=" + updateTimestamp +
                ", data=" + data +
                '}';
    }
}

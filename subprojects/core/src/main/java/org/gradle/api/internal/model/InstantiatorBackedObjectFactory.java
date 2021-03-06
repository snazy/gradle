/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.model;

import org.gradle.api.DomainObjectSet;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.provider.DefaultPropertyState;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.reflect.ObjectInstantiationException;
import org.gradle.internal.reflect.Instantiator;

public class InstantiatorBackedObjectFactory implements ObjectFactory {
    private final Instantiator instantiator;

    public InstantiatorBackedObjectFactory(Instantiator instantiator) {
        this.instantiator = instantiator;
    }

    @Override
    public <T extends Named> T named(Class<T> type, String name) throws ObjectInstantiationException {
        throw new UnsupportedOperationException("This ObjectFactory implementation does not support constructing named objects");
    }

    @Override
    public SourceDirectorySet sourceDirectorySet(String name, String displayName) {
        throw new UnsupportedOperationException("This ObjectFactory implementation does not support constructing source directory sets");
    }

    @Override
    public ConfigurableFileCollection fileCollection() {
        throw new UnsupportedOperationException("This ObjectFactory implementation does not support constructing file collections");
    }

    @Override
    public <T> NamedDomainObjectContainer<T> domainObjectContainer(Class<T> elementType) {
        throw new UnsupportedOperationException("This ObjectFactory implementation does not support constructing named domain object container");
    }

    @Override
    public <T> NamedDomainObjectContainer<T> domainObjectContainer(Class<T> elementType, NamedDomainObjectFactory<T> factory) {
        throw new UnsupportedOperationException("This ObjectFactory implementation does not support constructing named domain object container with factory");
    }

    @Override
    public <T> DomainObjectSet<T> domainObjectSet(Class<T> elementType) {
        throw new UnsupportedOperationException("This ObjectFactory implementation does not support constructing domain object set");
    }

    @Override
    public <T> Property<T> property(Class<T> valueType) {
        return new DefaultPropertyState<T>(valueType);
    }

    @Override
    public <T> ListProperty<T> listProperty(Class<T> elementType) {
        return broken();
    }

    @Override
    public <T> SetProperty<T> setProperty(Class<T> elementType) {
        return broken();
    }

    @Override
    public <K, V> MapProperty<K, V> mapProperty(Class<K> keyType, Class<V> valueType) {
        return broken();
    }

    @Override
    public DirectoryProperty directoryProperty() {
        return broken();
    }

    @Override
    public RegularFileProperty fileProperty() {
        return broken();
    }

    private <T> T broken() {
        throw new UnsupportedOperationException("This ObjectFactory implementation does not support constructing property objects");
    }

    @Override
    public <T> T newInstance(Class<? extends T> type, Object... parameters) throws ObjectInstantiationException {
        return instantiator.newInstance(type, parameters);
    }
}

package com.company.starter.mapstruct.filter;

import com.company.starter.mapstruct.MapstructMapper;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.InterfaceTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.io.InputStream;

public class MapstructTypeFilter implements TypeFilter {

    // Descripteur ASM interne JVM de @org.mapstruct.Mapper
    private static final String MAPPER_DESCRIPTOR = "Lorg/mapstruct/Mapper;";

    private final TypeFilter byInterface = new InterfaceTypeFilter(MapstructMapper.class);

    @Override
    public boolean match(MetadataReader metadataReader,
                         MetadataReaderFactory metadataReaderFactory) throws IOException {
        return byInterface.match(metadataReader, metadataReaderFactory)
            || hasMapperAnnotationInBytecode(metadataReader, metadataReaderFactory);
    }

    private boolean hasMapperAnnotationInBytecode(MetadataReader metadataReader,
                                                   MetadataReaderFactory metadataReaderFactory)
            throws IOException {
        // Cas 1 : annotation directe sur la classe/interface
        if (hasMapperAnnotation(metadataReader.getResource().getInputStream())) {
            return true;
        }

        // Cas 2 : remonte les super-interfaces récursivement
        return hasMapperAnnotationOnSuperInterfaces(
                metadataReader.getClassMetadata().getInterfaceNames(),
                metadataReaderFactory
        );
    }

    /**
     * Lit le bytecode via ClassVisitor ASM embarqué dans Spring Core.
     * Détecte @Mapper qu'elle soit @Retention(CLASS) ou @Retention(RUNTIME).
     * visible=false → CLASS, visible=true → RUNTIME : on traite les deux.
     */
    private boolean hasMapperAnnotation(InputStream classInputStream) throws IOException {
        try (InputStream is = classInputStream) {
            MapperAnnotationDetector detector = new MapperAnnotationDetector();
            new ClassReader(is).accept(
                    detector,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES
            );
            return detector.isMapperFound();
        }
    }

    private boolean hasMapperAnnotationOnSuperInterfaces(String[] interfaceNames,
                                                          MetadataReaderFactory factory)
            throws IOException {
        for (String interfaceName : interfaceNames) {
            try {
                MetadataReader ifaceReader = factory.getMetadataReader(interfaceName);

                if (hasMapperAnnotation(ifaceReader.getResource().getInputStream())) {
                    return true;
                }

                String[] superInterfaces = ifaceReader.getClassMetadata().getInterfaceNames();
                if (superInterfaces.length > 0
                        && hasMapperAnnotationOnSuperInterfaces(superInterfaces, factory)) {
                    return true;
                }
            } catch (IOException ignored) {
                // Interface absente du classpath (ex: java.io.Serializable) → on ignore
            }
        }
        return false;
    }

    /**
     * ClassVisitor minimal — s'arrête dès que @Mapper est trouvé.
     */
    private static class MapperAnnotationDetector extends ClassVisitor {

        private boolean mapperFound = false;

        MapperAnnotationDetector() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (MAPPER_DESCRIPTOR.equals(descriptor)) {
                mapperFound = true;
            }
            return null;
        }

        boolean isMapperFound() {
            return mapperFound;
        }
    }
}

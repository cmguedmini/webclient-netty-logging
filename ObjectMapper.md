if (objectMapper != null) {
            builder.messageConverters(converters -> {
                for (var converter : converters) {
                    if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                        jacksonConverter.setObjectMapper(objectMapper);
                    }
                }
            });
        }

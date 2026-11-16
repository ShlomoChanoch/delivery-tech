package com.deliverytech.delivery_api.exception;

public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String entity, Object id) {
        super(String.format("%s com ID %s não encontrado", entity, id), "entity.not.found");
    }
}

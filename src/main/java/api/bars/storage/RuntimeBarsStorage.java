package api.bars.storage;

import lombok.Data;

/** Хранилище сессии БАРС в памяти; используется по умолчанию. */
@Data
public class RuntimeBarsStorage implements BarsStorage {

    private volatile String authorization;
}

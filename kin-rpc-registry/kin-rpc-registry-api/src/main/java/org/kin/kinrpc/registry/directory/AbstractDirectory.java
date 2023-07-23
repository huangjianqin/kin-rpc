package org.kin.kinrpc.registry.directory;

import org.kin.kinrpc.registry.DirectoryListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author huangjianqin
 * @date 2023/7/23
 */
public abstract class AbstractDirectory implements Directory {
    /** {@link DirectoryListener}实例 */
    private final List<DirectoryListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public final void addListener(DirectoryListener listener) {
        listeners.add(listener);
    }

    //getter
    public List<DirectoryListener> getListeners() {
        return listeners;
    }
}

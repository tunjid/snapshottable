package com.tunjid.snapshottable.compat.k250_dev_7307

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k250_dev_6460.CompatContextImpl as DelegateType

// 2.5.0-dev-7307 does not affect the snapshottable compat surface.
public class CompatContextImpl : CompatContext by DelegateType() {

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.5.0-dev-7307"
        override fun create(): CompatContext = CompatContextImpl()
    }
}

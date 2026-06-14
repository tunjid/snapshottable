package com.tunjid.snapshottable.compat.k2420_dev_3583

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k240.CompatContextImpl as DelegateType

// 2.4.20-dev-3583 churned APIs that do not sit on the snapshottable compat surface. Metro re-pins
// IrGeneratedDeclarationsRegistrar creation, the plugin-generated KtFakeSourceElementKind, and
// IrElement.dumpKotlinLike at this version; none of those are wrapped by snapshottable. This
// module is therefore a pure chain link: it exists so factory selection prefers it over k240 when
// a host bundles a 2.4.20 dev compiler, and as a hook for any 2.4.20-specific snapshottable APIs
// we might add later.
public open class CompatContextImpl : CompatContext by DelegateType() {

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.4.20-dev-3583"
        override fun create(): CompatContext = CompatContextImpl()
    }
}

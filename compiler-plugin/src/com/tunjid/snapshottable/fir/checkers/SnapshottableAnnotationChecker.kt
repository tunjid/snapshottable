package com.tunjid.snapshottable.fir.checkers

import com.tunjid.snapshottable.fir.filters
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInterface

object SnapshottableAnnotationChecker : FirClassChecker(
    MppCheckerKind.Common,
) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) = with(context.session.filters) {
        when {
            isSnapshottableInterface(declaration.classId) -> {
                when {
                    !declaration.isInterface ->
                        reporter.reportOn(
                            source = declaration.source,
                            factory = SnapshottableErrors.NOT_SNAPSHOTTABLE_INTERFACE,
                        )

                    snapshottableInterfaceIdToSpecSymbol(declaration.classId) == null ->
                        reporter.reportOn(
                            source = declaration.source,
                            factory = SnapshottableErrors.NO_SNAPSHOTTABLE_SPEC,
                        )
                }
            }

            isSnapshottableSpec(declaration.classId) -> {
                val primaryConstructor = declaration.primaryConstructorIfAny(context.session)
                when {
                    primaryConstructor == null ->
                        reporter.reportOn(
                            source = declaration.source,
                            factory = SnapshottableErrors.NO_PRIMARY_CONSTRUCTOR,
                        )

                    primaryConstructor.rawStatus.visibility == Visibilities.Private ->
                        reporter.reportOn(
                            source = primaryConstructor.source,
                            factory = SnapshottableErrors.PRIVATE_CONSTRUCTOR,
                        )

                    declaration.isAbstract || !declaration.isFinal ->
                        reporter.reportOn(
                            source = declaration.source,
                            factory = SnapshottableErrors.NOT_SNAPSHOTTABLE_SPEC,
                        )

                    nestedClassIdToSnapshottableInterfaceClassId(declaration.classId) == null ->
                        reporter.reportOn(
                            source = declaration.source,
                            factory = SnapshottableErrors.NO_SNAPSHOTTABLE_INTERFACE,
                        )
                }
            }
        }
    }
}

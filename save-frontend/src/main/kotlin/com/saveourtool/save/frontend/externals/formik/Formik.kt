@file:JsModule("formik")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.formik

import react.Component
import react.ComponentType
import react.ElementType
import react.PropsWithChildren
import react.ReactElement
import react.ReactNode
import react.State
import react.dom.events.ChangeEvent
import react.dom.events.FormEvent

external interface FormikProps : PropsWithChildren {
    var initialValues: dynamic
    var values: dynamic
    var handleChange: (ChangeEvent<*>) -> Unit
    var handleSubmit: (FormEvent<*>) -> Unit
    var handleReset: (Any) -> Unit
}

external interface FormProps : PropsWithChildren

external interface FieldProps : PropsWithChildren {
//    var id: dynamic
//    var name: dynamic
//    var placeholder: dynamic
//    var type: dynamic
    var field: dynamic
    var form: dynamic
    var meta: dynamic
}
//
//@JsName("Formik")
//external class Formik : Component<FormikProps, State> {
//    override fun render(): ReactNode?
//}

@JsName("Formik")
external fun Formik(fc: FormikConfig): ReactElement<*>

//external class Formik : Component<PropsWithChildren, State> {
//    override fun render(): ReactNode?
//}

external class FormikConfig {
    var initialValues: dynamic
    var validate: (dynamic) -> dynamic
    var handleChange: (ChangeEvent<*>) -> Unit
    var onReset: (values: dynamic, formikBag: dynamic) -> Unit
    var enableReinitialize: Boolean
    var children: ((formikProps: FormikProps) -> ReactNode)?
    var component: ComponentType<FormikProps>?
}

@JsName("Form")
external class Form : Component<FormProps, State> {
    override fun render(): ReactNode?
}

@JsName("Field")
external fun Field(fc: FieldConfig): ReactElement<*>
//external class Field : Component<FieldProps, State> {
//    override fun render(): ReactNode?
//}

external class FieldConfig {
    var name: String
    var component: dynamic
    var children: dynamic
}

@JsName("useFormik")
external fun useFormik(props: FormikProps): dynamic

@JsName("withFormik")
external fun withFormik(withFormikConfig: dynamic): (elementType: ElementType<*>) -> ElementType<*>
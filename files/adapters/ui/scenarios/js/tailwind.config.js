import colors from 'tailwindcss/colors'
import typography from '@tailwindcss/typography'
import forms from '@tailwindcss/forms'

/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./target/scala-3*/scenarios-*opt/*.js"
    ],
    theme: {
        extend: {
            fontFamily: {
                serif: ['Inter', 'ui-serif', 'Georgia', 'Cambria', '"Times New Roman"', 'Times', 'serif'],
            },
            colors: {
                gray: colors.gray,
                indigo: colors.blue
            },
        },
    },
    plugins: [typography, forms],
}


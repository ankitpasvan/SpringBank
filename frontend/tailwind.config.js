/** @type {import('tailwindcss').Config} */
module.exports = {
    content: ['./src/**/*.{js,jsx}'],
    theme: {
        extend: {
            colors: {
                brand: {
                    50: '#f0fdf4',
                    100: '#dcfce7',
                    200: '#bbf7d0',
                    300: '#86efac',
                    400: '#4ade80',
                    500: '#22c55e',
                    600: '#16a34a',
                    700: '#15803d',
                    800: '#166534',
                    900: '#14532d',
                },
                lime: { accent: '#9fe870' },
                ink: '#163300',
                mist: '#f7f8f7',
            },
            fontFamily: {
                sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
            },
            boxShadow: {
                card: '0 1px 2px rgba(22, 51, 0, 0.05), 0 4px 16px rgba(22, 51, 0, 0.06)',
                lift: '0 4px 12px rgba(22, 51, 0, 0.08), 0 12px 32px rgba(22, 51, 0, 0.10)',
            },
        },
    },
    plugins: [],
};

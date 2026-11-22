import './globals.css'
import { AuthProvider } from '@/context/AuthContext.jsx'
import { ToastProvider } from '@/context/ToastContext.jsx'

export const metadata = {
    title: 'SpringBank — Modern Banking, Made Simple',
    description:
        'SpringBank is a simple, secure way to manage your money online. Open an account, deposit, withdraw and send money — with a clear record of every transaction.',
}

export default function RootLayout({ children }) {
    return (
        <html lang="en">
        <body>
        <ToastProvider>
            <AuthProvider>{children}</AuthProvider>
        </ToastProvider>
        </body>
        </html>
    )
}

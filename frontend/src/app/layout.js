import './globals.css';

export const metadata = {
    title: 'SpringBank — Modern Banking, Made Simple',
    description:
        'SpringBank is a simple, secure way to manage your money online. Open an account, deposit, withdraw and send money — with a clear record of every transaction.',
};

export default function RootLayout({ children }) {
    return (
        <html lang="en">
        <body>{children}</body>
        </html>
    );
}

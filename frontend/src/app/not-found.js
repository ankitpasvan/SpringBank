import Link from 'next/link'

export default function NotFound() {
    return (
        <div className="flex min-h-screen flex-col items-center justify-center bg-mist px-4 text-center">
            <p className="text-6xl font-extrabold tracking-tight text-ink">404</p>
            <h1 className="mt-3 text-2xl font-extrabold tracking-tight">Page not found</h1>
            <p className="mt-2 max-w-sm text-slate-600">
                The page you are looking for does not exist or has moved.
            </p>
            <Link
                href="/"
                className="mt-6 inline-flex items-center gap-2 rounded-full bg-brand-600 px-6 py-3 font-semibold text-white transition hover:bg-brand-700"
            >
                Back to home
            </Link>
        </div>
    )
}

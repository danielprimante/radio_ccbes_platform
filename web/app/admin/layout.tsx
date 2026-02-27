'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import Link from 'next/link';
import AdminGuard from '@/components/AdminGuard';

export default function AdminLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    const [user, setUser] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const router = useRouter();
    const pathname = usePathname();

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
            if (currentUser) {
                setUser(currentUser);
            }
            setLoading(false);
        });

        return () => unsubscribe();
    }, []);

    const handleLogout = async () => {
        await auth.signOut();
        router.push('/login');
    };

    const navItems = [
        { name: 'Dashboard', href: '/admin' },
        { name: 'Moderación', href: '/admin/moderation' },
        { name: 'Usuarios', href: '/admin/users' },
        { name: 'Programas', href: '/admin/programs' },
        { name: 'Landing Page', href: '/admin/web-configuration' },
        { name: 'Configuracion App', href: '/admin/settings' },
    ];


    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-gray-600 font-medium">Cargando panel...</div>
            </div>
        );
    }

    return (
        <AdminGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                {/* Header compartido */}
                <header className="bg-white shadow-sm sticky top-0 z-50">
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center">
                        <div className="flex items-center gap-8">
                            <h1 className="text-xl md:text-2xl font-bold text-gray-900">Radio CCBES</h1>
                            <nav className="hidden md:flex gap-4">
                                {navItems.map((item) => {
                                    const isActive = pathname === item.href;
                                    return (
                                        <Link
                                            key={item.href}
                                            href={item.href}
                                            className={`${isActive
                                                ? 'text-red-600 font-semibold border-b-2 border-red-600'
                                                : 'text-gray-600 hover:text-red-600 font-medium transition-colors'
                                                } pb-1`}
                                        >
                                            {item.name}
                                        </Link>
                                    );
                                })}
                            </nav>
                        </div>
                        <div className="flex items-center gap-2 md:gap-4">
                            {/* Botón Burger para Mobile */}
                            <button
                                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                                className="md:hidden p-2 rounded-lg hover:bg-gray-100 transition-colors"
                                aria-label="Abrir menú"
                            >
                                <svg className="w-6 h-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    {mobileMenuOpen ? (
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                    ) : (
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                                    )}
                                </svg>
                            </button>

                            <div className="hidden sm:block text-right">
                                <p className="text-xs text-gray-400 font-medium">Administrador</p>
                                <p className="text-sm text-gray-600 leading-tight truncate max-w-[150px]">{user?.email}</p>
                            </div>
                            <button
                                onClick={handleLogout}
                                className="bg-gray-100 text-gray-700 px-2 md:px-3 py-1.5 rounded-lg text-xs md:text-sm font-semibold hover:bg-red-50 hover:text-red-600 transition-all border border-gray-200"
                            >
                                <span className="hidden sm:inline">Cerrar Sesión</span>
                                <span className="sm:hidden">Salir</span>
                            </button>
                        </div>
                    </div>

                    {/* Menú Mobile */}
                    {mobileMenuOpen && (
                        <div className="md:hidden border-t border-gray-200 bg-white">
                            <nav className="px-4 py-3 space-y-1">
                                {navItems.map((item) => {
                                    const isActive = pathname === item.href;
                                    return (
                                        <Link
                                            key={item.href}
                                            href={item.href}
                                            onClick={() => setMobileMenuOpen(false)}
                                            className={`block px-4 py-3 rounded-lg font-medium transition-colors ${isActive
                                                ? 'bg-red-50 text-red-600 font-semibold'
                                                : 'text-gray-700 hover:bg-gray-50 hover:text-red-600'
                                                }`}
                                        >
                                            {item.name}
                                        </Link>
                                    );
                                })}
                                <div className="pt-3 border-t border-gray-100 sm:hidden">
                                    <p className="px-4 text-xs text-gray-400 font-medium">Administrador</p>
                                    <p className="px-4 text-sm text-gray-600 leading-tight truncate">{user?.email}</p>
                                </div>
                            </nav>
                        </div>
                    )}
                </header>

                {/* Contenido de la página */}
                <main className="flex-grow">
                    {children}
                </main>

                <footer className="bg-white border-t border-gray-100 py-6 mt-auto">
                    <div className="max-w-7xl mx-auto px-4 text-center">
                        <p className="text-gray-400 text-xs">
                            © {new Date().getFullYear()} Radio CCBES Panel Administrativo
                        </p>
                    </div>
                </footer>
            </div>
        </AdminGuard>
    );
}

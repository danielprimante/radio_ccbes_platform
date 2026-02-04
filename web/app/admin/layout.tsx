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
        { name: 'Landing Page', href: '/admin/web-configuration' },
        { name: 'Configuración', href: '/admin/settings' },
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
                <header className="bg-white shadow-sm sticky top-0 z-10">
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center">
                        <div className="flex items-center gap-8">
                            <h1 className="text-2xl font-bold text-gray-900">Radio CCBES</h1>
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
                        <div className="flex items-center gap-4">
                            <div className="hidden sm:block text-right">
                                <p className="text-xs text-gray-400 font-medium">Administrador</p>
                                <p className="text-sm text-gray-600 leading-tight">{user?.email}</p>
                            </div>
                            <button
                                onClick={handleLogout}
                                className="bg-gray-100 text-gray-700 px-3 py-1.5 rounded-lg text-sm font-semibold hover:bg-red-50 hover:text-red-600 transition-all border border-gray-200"
                            >
                                Cerrar Sesión
                            </button>
                        </div>
                    </div>
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

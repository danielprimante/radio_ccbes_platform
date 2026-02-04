'use client';

import { useEffect, useState } from 'react';

export default function NotFound() {
    const [status, setStatus] = useState('Buscando...');
    const [postId, setPostId] = useState<string | null>(null);
    const [userId, setUserId] = useState<string | null>(null);

    useEffect(() => {
        const path = window.location.pathname;
        const pathSegments = path.split('/').filter(Boolean);

        if (pathSegments.length >= 2) {
            const type = pathSegments[0];
            const id = pathSegments[1];

            if (type === 'post') {
                setPostId(id);
                setStatus('Abriendo publicación en la App...');
                const appIntent = `intent://ccbes.com.ar/post/${id}#Intent;scheme=https;package=com.radio.ccbes;S.browser_fallback_url=${encodeURIComponent(window.location.href)};end`;
                window.location.href = appIntent;
            } else if (type === 'profile') {
                setUserId(id);
                setStatus('Abriendo perfil en la App...');
                const appIntent = `intent://ccbes.com.ar/profile/${id}#Intent;scheme=https;package=com.radio.ccbes;S.browser_fallback_url=${encodeURIComponent(window.location.href)};end`;
                window.location.href = appIntent;
            } else {
                setStatus('Página no encontrada');
            }
        } else {
            setStatus('Página no encontrada');
        }
    }, []);

    const handleRetry = () => {
        const id = postId || userId;
        const type = postId ? 'post' : 'profile';
        if (id) {
            const appIntent = `intent://ccbes.com.ar/${type}/${id}#Intent;scheme=https;package=com.radio.ccbes;end`;
            window.location.href = appIntent;
        }
    };

    return (
        <div className="flex flex-col items-center justify-center min-h-screen p-4 text-center bg-white text-gray-900">
            <div className="max-w-md w-full">
                <h1 className="text-2xl font-bold mb-4">{status}</h1>

                {(postId || userId) ? (
                    <>
                        <p className="text-gray-600 mb-8">
                            Si la aplicación Radio CCBES no se abre automáticamente, haz clic en el botón de abajo.
                        </p>

                        <button
                            onClick={handleRetry}
                            className="inline-block px-8 py-4 bg-red-600 text-white rounded-full font-bold shadow-lg hover:bg-red-700 transition-colors"
                        >
                            ABRIR EN LA APP
                        </button>
                    </>
                ) : (
                    <>
                        <p className="text-gray-600 mb-8">
                            La página que buscas no existe o ha sido movida.
                        </p>
                        <a href="/" className="text-red-600 font-bold hover:underline">
                            Volver al inicio
                        </a>
                    </>
                )}

                <div className="mt-12 text-sm text-gray-400">
                    CCBES Mar del Plata
                </div>
            </div>
        </div>
    );
}

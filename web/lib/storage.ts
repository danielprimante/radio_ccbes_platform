// Usamos ImgBB en lugar de Firebase Storage para evitar costos
const IMGBB_API_KEY = "d7d8c8028977e32ee9fab67b251c6697";

/**
 * Sube una imagen a ImgBB
 * @param file - El archivo de imagen a subir
 * @param path - No se usa en ImgBB (mantenido por compatibilidad de firma)
 * @returns Promise con la URL de descarga directa
 */
export interface ImgBBData {
    id: string;
    url: string;
    delete_url: string;
}

/**
 * Sube una imagen a ImgBB
 * @param file - El archivo de imagen a subir
 * @param path - No se usa en ImgBB (mantenido por compatibilidad de firma)
 * @returns Promise con los datos de la imagen (id, url, delete_url)
 */
export async function uploadImage(file: File, path: string = 'posts'): Promise<ImgBBData> {
    try {
        const formData = new FormData();
        formData.append('image', file);

        console.log('Subiendo imagen a ImgBB...');

        const response = await fetch(`https://api.imgbb.com/1/upload?key=${IMGBB_API_KEY}`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error?.message || 'Error en la respuesta de ImgBB');
        }

        const data = await response.json();
        const imgData = data.data;

        console.log('Imagen subida con éxito a ImgBB:', imgData.url);
        return {
            id: imgData.id,
            url: imgData.url,
            delete_url: imgData.delete_url
        };
    } catch (error) {
        console.error('Error uploading image to ImgBB:', error);
        throw new Error('Error al subir la imagen a ImgBB');
    }
}

/**
 * Elimina una imagen de ImgBB usando su delete_url
 * @param deleteUrl - La URL de borrado devuelta por ImgBB (e.g., https://ibb.co/ID/HASH)
 */
export async function deleteImage(deleteUrl: string): Promise<void> {
    try {
        console.log('Intentando borrar imagen de ImgBB:', deleteUrl);

        // Parse deleteUrl: https://ibb.co/ID/HASH
        const regex = /https:\/\/ibb\.co\/([a-zA-Z0-9]+)\/([a-zA-Z0-9]+)/;
        const match = deleteUrl.match(regex);

        if (!match) {
            console.error('Formato de delete_url inválido:', deleteUrl);
            return;
        }

        const imageId = match[1];
        const imageHash = match[2];

        const formData = new FormData();
        formData.append("action", "delete");
        formData.append("delete", "image");
        formData.append("from", "resource");
        formData.append("deleting[id]", imageId);
        formData.append("deleting[hash]", imageHash);

        // Usamos fetch directo al endpoint de borrado extraoficial
        await fetch("https://ibb.co/json", {
            method: "POST",
            body: formData,
        });

        console.log('Imagen eliminada de ImgBB (o solicitud enviada)');
    } catch (error) {
        console.error('Error deleting image from ImgBB:', error);
        // No lanzamos error para no bloquear el flujo principal de borrado de datos
    }
}

/**
 * Comprime un archivo de imagen antes de subirlo
 */
export async function compressImage(
    file: File,
    maxWidth: number = 1200,
    quality: number = 0.8
): Promise<File> {
    return new Promise((resolve, reject) => {
        const objectUrl = URL.createObjectURL(file);
        const img = new Image();
        img.src = objectUrl;

        img.onload = () => {
            URL.revokeObjectURL(objectUrl);
            const canvas = document.createElement('canvas');
            let width = img.width;
            let height = img.height;

            if (width > maxWidth) {
                height = (maxWidth / width) * height;
                width = maxWidth;
            }

            canvas.width = width;
            canvas.height = height;

            const ctx = canvas.getContext('2d');
            if (!ctx) {
                reject(new Error('No se pudo obtener el contexto del canvas'));
                return;
            }

            ctx.drawImage(img, 0, 0, width, height);

            // Determine output format and whether to preserve transparency
            const isTransparent = file.type === 'image/png' || file.type === 'image/webp';
            // We use webp for transparency + compression if supported, 
            // otherwise we stick to the original type for transparent images.
            // For others (mostly JPEG), we keep using JPEG.
            const outputType = isTransparent ? 'image/webp' : 'image/jpeg';

            canvas.toBlob(
                (blob) => {
                    if (blob) {
                        const compressedFile = new File([blob], file.name, {
                            type: outputType,
                            lastModified: Date.now(),
                        });
                        resolve(compressedFile);
                    } else {
                        reject(new Error('Error al comprimir la imagen'));
                    }
                },
                outputType,
                quality
            );
        };

        img.onerror = (error) => {
            URL.revokeObjectURL(objectUrl);
            reject(error);
        };
    });
}

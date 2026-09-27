import apiClient from './api';

/**
 * Service for document text extraction (TXT, PDF, DOCX).
 * Uploads documents to Spring Boot backend without permanent storage.
 */
export const documentService = {
  /**
   * Upload document and extract text content.
   * POST /api/documents/extract-text
   *
   * @param {File} file - Document file to upload
   * @returns {Promise<{
   *   success: boolean,
   *   text: string,
   *   filename: string,
   *   fileType: string,
   *   characterCount: number,
   *   wordCount: number,
   *   fileSize: number,
   *   message: string
   * }>}
   */
  async extractText(file) {
    if (!file) {
      throw {
        userFriendlyMessage: 'Please select a file to extract text from.'
      };
    }

    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post('/api/documents/extract-text', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });

    return response.data;
  }
};

export default documentService;

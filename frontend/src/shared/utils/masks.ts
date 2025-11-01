/**
 * Input mask utilities
 */

export const masks = {
  /**
   * CPF mask: 000.000.000-00
   */
  cpf: (value: string): string => {
    return value
      .replace(/\D/g, '')
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d{1,2})/, '$1-$2')
      .replace(/(-\d{2})\d+?$/, '$1');
  },

  /**
   * CNPJ mask: 00.000.000/0000-00
   */
  cnpj: (value: string): string => {
    return value
      .replace(/\D/g, '')
      .replace(/(\d{2})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1/$2')
      .replace(/(\d{4})(\d)/, '$1-$2')
      .replace(/(-\d{2})\d+?$/, '$1');
  },

  /**
   * Phone mask: (00) 0000-0000 or (00) 00000-0000
   */
  phone: (value: string): string => {
    const cleaned = value.replace(/\D/g, '');

    if (cleaned.length <= 10) {
      return cleaned
        .replace(/(\d{2})(\d)/, '($1) $2')
        .replace(/(\d{4})(\d)/, '$1-$2')
        .replace(/(-\d{4})\d+?$/, '$1');
    }

    return cleaned
      .replace(/(\d{2})(\d)/, '($1) $2')
      .replace(/(\d{5})(\d)/, '$1-$2')
      .replace(/(-\d{4})\d+?$/, '$1');
  },

  /**
   * CEP mask: 00000-000
   */
  cep: (value: string): string => {
    return value
      .replace(/\D/g, '')
      .replace(/(\d{5})(\d)/, '$1-$2')
      .replace(/(-\d{3})\d+?$/, '$1');
  },

  /**
   * Remove all non-digit characters
   */
  onlyNumbers: (value: string): string => {
    return value.replace(/\D/g, '');
  },

  /**
   * CPF/CNPJ auto mask (detects which to use based on length)
   */
  cpfCnpj: (value: string): string => {
    const cleaned = value.replace(/\D/g, '');

    if (cleaned.length <= 11) {
      return masks.cpf(value);
    }

    return masks.cnpj(value);
  },

  /**
   * Placa de veículo mask: ABC-1234 ou ABC1D23 (Mercosul)
   */
  placa: (value: string): string => {
    const cleaned = value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();

    // Formato antigo: ABC-1234
    if (cleaned.length <= 7) {
      return cleaned
        .replace(/^([A-Z]{0,3})/, '$1')
        .replace(/^([A-Z]{3})([0-9]{0,4})/, '$1-$2');
    }

    // Formato Mercosul: ABC1D23
    return cleaned.substring(0, 7);
  },

  /**
   * Chassi (VIN) mask: 17 caracteres alfanuméricos (sem I, O, Q)
   */
  chassi: (value: string): string => {
    return value
      .replace(/[^A-HJ-NPR-Z0-9]/gi, '')
      .toUpperCase()
      .substring(0, 17);
  },
};

/**
 * Remove mask from value (keep only numbers)
 */
export const removeMask = (value: string): string => {
  return value.replace(/\D/g, '');
};

/**
 * Validate CPF
 */
export const validateCPF = (cpf: string): boolean => {
  const cleaned = removeMask(cpf);

  if (cleaned.length !== 11) return false;
  if (/^(\d)\1+$/.test(cleaned)) return false; // All same digits

  let sum = 0;
  let remainder;

  for (let i = 1; i <= 9; i++) {
    sum += parseInt(cleaned.substring(i - 1, i)) * (11 - i);
  }

  remainder = (sum * 10) % 11;
  if (remainder === 10 || remainder === 11) remainder = 0;
  if (remainder !== parseInt(cleaned.substring(9, 10))) return false;

  sum = 0;
  for (let i = 1; i <= 10; i++) {
    sum += parseInt(cleaned.substring(i - 1, i)) * (12 - i);
  }

  remainder = (sum * 10) % 11;
  if (remainder === 10 || remainder === 11) remainder = 0;
  if (remainder !== parseInt(cleaned.substring(10, 11))) return false;

  return true;
};

/**
 * Validate CNPJ
 */
export const validateCNPJ = (cnpj: string): boolean => {
  const cleaned = removeMask(cnpj);

  if (cleaned.length !== 14) return false;
  if (/^(\d)\1+$/.test(cleaned)) return false; // All same digits

  let length = cleaned.length - 2;
  let numbers = cleaned.substring(0, length);
  const digits = cleaned.substring(length);
  let sum = 0;
  let pos = length - 7;

  for (let i = length; i >= 1; i--) {
    sum += parseInt(numbers.charAt(length - i)) * pos--;
    if (pos < 2) pos = 9;
  }

  let result = sum % 11 < 2 ? 0 : 11 - (sum % 11);
  if (result !== parseInt(digits.charAt(0))) return false;

  length = length + 1;
  numbers = cleaned.substring(0, length);
  sum = 0;
  pos = length - 7;

  for (let i = length; i >= 1; i--) {
    sum += parseInt(numbers.charAt(length - i)) * pos--;
    if (pos < 2) pos = 9;
  }

  result = sum % 11 < 2 ? 0 : 11 - (sum % 11);
  if (result !== parseInt(digits.charAt(1))) return false;

  return true;
};
